package com.jpy.wordbook.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ConversationMessage(
    val role: String,  // "user" or "assistant"
    val japanese: String,
    val english: String? = null
)

data class ConversationResponse(
    val japanese: String,
    val english: String
)

data class Conversation(
    val id: String,
    val scenario: String,
    val messages: MutableList<ConversationMessage> = mutableListOf()
)

@Singleton
class ConversationService(
    @Client("\${ollama.base-url}") private val httpClient: HttpClient,
    @Value("\${ollama.model}") private val model: String,
    private val vocabularyService: VocabularyService
) {
    private val logger = LoggerFactory.getLogger(ConversationService::class.java)
    private val objectMapper = jacksonObjectMapper()
    private val conversations = ConcurrentHashMap<String, Conversation>()

    // JLPT N5 core vocabulary for conversation guidance
    private val jlptN5Guidance = """
        Use JLPT N5 level vocabulary and grammar. Common words to use:
        - Pronouns: 私(わたし), あなた, 彼(かれ), 彼女(かのじょ)
        - Verbs: する, ある, いる, 行く(いく), 来る(くる), 食べる(たべる), 飲む(のむ), 見る(みる), 聞く(きく), 話す(はなす), 読む(よむ), 書く(かく), 買う(かう), 分かる(わかる)
        - Adjectives: 大きい, 小さい, 新しい, 古い, 良い, 悪い, 高い, 安い, 美味しい(おいしい)
        - Time: 今日(きょう), 明日(あした), 昨日(きのう), 今(いま), 朝(あさ), 昼(ひる), 夜(よる)
        - Places: 駅(えき), 店(みせ), 学校(がっこう), 会社(かいしゃ), 家(いえ), レストラン, ホテル
        - Question words: 何(なに), どこ, いつ, 誰(だれ), どう, いくら
        - Particles: は, が, を, に, で, と, も, から, まで
        - Common phrases: すみません, ありがとう, お願いします, はい, いいえ
    """.trimIndent()

    fun startConversation(scenario: String): Pair<String, ConversationResponse> {
        val id = UUID.randomUUID().toString()
        val conversation = Conversation(id = id, scenario = scenario)

        val greeting = getGreetingForScenario(scenario)
        conversation.messages.add(ConversationMessage("assistant", greeting.japanese, greeting.english))

        conversations[id] = conversation
        return Pair(id, greeting)
    }

    fun processUserMessage(conversationId: String, userJapanese: String): ConversationResponse {
        val conversation = conversations[conversationId]
            ?: throw IllegalArgumentException("Conversation not found")

        // Add user message
        conversation.messages.add(ConversationMessage("user", userJapanese))

        // Get known words for context (sample from vocabulary)
        val knownWords = vocabularyService.getRandomWords(30)
            .joinToString(", ") { it.japanese }

        // Generate response
        val response = generateResponse(conversation, knownWords)

        // Add assistant response
        conversation.messages.add(ConversationMessage("assistant", response.japanese, response.english))

        return response
    }

    private fun generateResponse(conversation: Conversation, knownWords: String): ConversationResponse {
        val prompt = buildConversationPrompt(conversation, knownWords)

        try {
            val request = OllamaRequest(
                model = model,
                prompt = prompt,
                stream = false
            )

            val httpRequest = HttpRequest.POST("/api/generate", request)
            val response = httpClient.toBlocking().retrieve(httpRequest, OllamaResponse::class.java)

            return parseConversationResponse(response.response)
        } catch (e: Exception) {
            logger.error("Failed to generate conversation response", e)
            return ConversationResponse(
                japanese = "すみません、わかりませんでした。もう一度お願いします。",
                english = "Sorry, I didn't understand. Please say that again."
            )
        }
    }

    private fun buildConversationPrompt(conversation: Conversation, knownWords: String): String {
        val scenarioContext = getScenarioContext(conversation.scenario)
        val history = conversation.messages.takeLast(6).joinToString("\n") { msg ->
            when (msg.role) {
                "user" -> "User: ${msg.japanese}"
                "assistant" -> "Assistant: ${msg.japanese}"
                else -> ""
            }
        }

        return """
            You are a friendly Japanese conversation partner for a beginner learner.

            Scenario: $scenarioContext

            $jlptN5Guidance

            The learner also knows these words from their vocabulary list: $knownWords

            Conversation so far:
            $history

            Guidelines:
            - Respond naturally in Japanese, staying in character for the scenario
            - Keep responses short (1-2 sentences) and simple
            - Use polite form (です/ます)
            - If the user makes a grammar mistake, gently continue the conversation (don't correct directly)
            - Ask follow-up questions to keep the conversation going
            - Stay at JLPT N5 level

            Respond in this exact JSON format only:
            {"japanese": "Your response in Japanese", "english": "English translation"}
        """.trimIndent()
    }

    private fun getGreetingForScenario(scenario: String): ConversationResponse {
        return when (scenario) {
            "restaurant" -> ConversationResponse(
                japanese = "いらっしゃいませ！何名様ですか？",
                english = "Welcome! How many people?"
            )
            "shopping" -> ConversationResponse(
                japanese = "いらっしゃいませ！何をお探しですか？",
                english = "Welcome! What are you looking for?"
            )
            "directions" -> ConversationResponse(
                japanese = "すみません、何かお探しですか？",
                english = "Excuse me, are you looking for something?"
            )
            "hotel" -> ConversationResponse(
                japanese = "いらっしゃいませ。ご予約はございますか？",
                english = "Welcome. Do you have a reservation?"
            )
            "introduction" -> ConversationResponse(
                japanese = "はじめまして！お名前は何ですか？",
                english = "Nice to meet you! What is your name?"
            )
            else -> ConversationResponse(
                japanese = "こんにちは！日本語で話しましょう。何について話しますか？",
                english = "Hello! Let's speak in Japanese. What shall we talk about?"
            )
        }
    }

    private fun getScenarioContext(scenario: String): String {
        return when (scenario) {
            "restaurant" -> "You are a waiter at a Japanese restaurant. Help the customer order food."
            "shopping" -> "You are a shop clerk. Help the customer find and buy items."
            "directions" -> "You are a helpful local. Help the tourist find their way."
            "hotel" -> "You are a hotel receptionist. Help the guest check in."
            "introduction" -> "You are meeting someone new. Have a friendly self-introduction conversation."
            else -> "You are having a casual conversation with a Japanese learner."
        }
    }

    private fun parseConversationResponse(response: String): ConversationResponse {
        return try {
            objectMapper.readValue(response)
        } catch (e: Exception) {
            logger.error("Failed to parse conversation response: $response", e)
            ConversationResponse(
                japanese = "すみません、エラーが発生しました。",
                english = "Sorry, an error occurred."
            )
        }
    }

    fun getConversation(id: String): Conversation? = conversations[id]
}
