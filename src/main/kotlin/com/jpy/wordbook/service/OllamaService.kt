package com.jpy.wordbook.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jpy.wordbook.model.Word
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

data class TaskContent(
    val japanese: String,
    val english: String
)

data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false,
    val format: String = "json"
)

data class OllamaResponse(
    val response: String
)

@Singleton
class OllamaService(
    @Client("\${ollama.base-url}") private val httpClient: HttpClient,
    @Value("\${ollama.model}") private val model: String
) {
    private val logger = LoggerFactory.getLogger(OllamaService::class.java)
    private val objectMapper = jacksonObjectMapper()

    fun generateSentence(words: List<Word>): TaskContent {
        val prompt = buildSentencePrompt(words)
        return generate(prompt)
    }

    fun generateLongerSentence(words: List<Word>): TaskContent {
        val prompt = buildLongerSentencePrompt(words)
        return generate(prompt)
    }

    fun generateStory(words: List<Word>): TaskContent {
        val prompt = buildStoryPrompt(words)
        return generate(prompt)
    }

    private fun buildSentencePrompt(words: List<Word>): String {
        val wordList = words.joinToString(", ") { it.japanese }
        return """
            Create a natural Japanese sentence that includes at least one of these words: $wordList

            Guidelines:
            - Write a simple, natural sentence (beginner level)
            - You may add common words (particles, verbs, adjectives) to make the sentence grammatically correct and natural
            - Use appropriate kanji with hiragana/katakana
            - Aim for 5-15 characters

            Respond in this exact JSON format only:
            {"japanese": "日本語の文", "english": "English translation"}
        """.trimIndent()
    }

    private fun buildLongerSentencePrompt(words: List<Word>): String {
        val wordList = words.joinToString(", ") { it.japanese }
        return """
            Create a natural Japanese sentence using one or more of these words: $wordList

            Guidelines:
            - Write an intermediate level sentence
            - Feel free to add common vocabulary to create a natural, meaningful sentence
            - Use appropriate kanji, hiragana, and katakana
            - Aim for 12-25 characters
            - Make it interesting or useful for daily life

            Respond in this exact JSON format only:
            {"japanese": "日本語の文", "english": "English translation"}
        """.trimIndent()
    }

    private fun buildStoryPrompt(words: List<Word>): String {
        val wordList = words.joinToString(", ") { it.japanese }
        return """
            Create a short Japanese story (2-4 sentences) incorporating these words: $wordList

            Guidelines:
            - Write at beginner to intermediate level
            - Add any vocabulary needed to make the story flow naturally
            - Use appropriate kanji, hiragana, and katakana
            - Make it coherent and engaging
            - Keep it concise but meaningful

            Respond in this exact JSON format only:
            {"japanese": "日本語の物語", "english": "English translation"}
        """.trimIndent()
    }

    private fun generate(prompt: String): TaskContent {
        try {
            val request = OllamaRequest(
                model = model,
                prompt = prompt,
                stream = false,
                format = "json"
            )

            val httpRequest = HttpRequest.POST("/api/generate", request)
            val response = httpClient.toBlocking().retrieve(httpRequest, OllamaResponse::class.java)

            return parseResponse(response.response)
        } catch (e: Exception) {
            logger.error("Failed to generate content from Ollama", e)
            // Return a fallback
            return TaskContent(
                japanese = "エラーが発生しました。",
                english = "An error occurred."
            )
        }
    }

    private fun parseResponse(response: String): TaskContent {
        return try {
            objectMapper.readValue(response)
        } catch (e: Exception) {
            logger.error("Failed to parse Ollama response: $response", e)
            TaskContent(
                japanese = "パースエラー",
                english = "Parse error"
            )
        }
    }
}
