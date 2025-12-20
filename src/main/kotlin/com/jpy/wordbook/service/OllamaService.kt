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

data class AnswerFeedback(
    val correct: Boolean,
    val feedback: String,
    val suggestion: String?
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

    fun checkAnswer(japaneseText: String, correctTranslation: String, userAnswer: String): AnswerFeedback {
        val prompt = buildCheckAnswerPrompt(japaneseText, correctTranslation, userAnswer)
        return generateFeedback(prompt)
    }

    private fun buildSentencePrompt(words: List<Word>): String {
        val wordList = words.joinToString(", ") { it.japanese }
        return """
            Create a natural Japanese sentence that MUST include this word: ${words.first().japanese}

            Guidelines:
            - Write a simple, natural sentence (beginner level)
            - The word "$wordList" MUST appear in the sentence exactly as written
            - You may add common words (particles, verbs, adjectives) to make the sentence grammatically correct
            - Use appropriate kanji with hiragana/katakana
            - Aim for 5-15 characters

            Respond in this exact JSON format only:
            {"japanese": "日本語の文", "english": "English translation"}
        """.trimIndent()
    }

    private fun buildLongerSentencePrompt(words: List<Word>): String {
        val wordList = words.joinToString(", ") { it.japanese }
        return """
            Create a natural Japanese sentence that MUST include at least one of these words: $wordList

            Guidelines:
            - Write an intermediate level sentence
            - At least one word from the list MUST appear exactly as written
            - You may add common vocabulary to make it natural and meaningful
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
            Create a short Japanese story using these words: $wordList

            IMPORTANT REQUIREMENTS:
            - The story MUST have exactly 3 sentences (use periods 。 to separate them)
            - At least one word from the list MUST appear exactly as written
            - Write at beginner to intermediate level
            - Use appropriate kanji, hiragana, and katakana
            - Make the 3 sentences form a coherent mini-story

            Example structure: [Setup sentence]。[Development sentence]。[Conclusion sentence]。

            Respond in this exact JSON format only:
            {"japanese": "文1。文2。文3。", "english": "English translation of all 3 sentences"}
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

    private fun buildCheckAnswerPrompt(japaneseText: String, correctTranslation: String, userAnswer: String): String {
        return """
            You are a Japanese language teacher checking a student's translation.

            Japanese text: $japaneseText
            Correct translation: $correctTranslation
            Student's answer: $userAnswer

            Compare the student's answer with the correct translation. Consider:
            - The core meaning is captured
            - Minor wording differences are acceptable if the meaning is preserved
            - Grammar and phrasing don't need to be identical

            Provide friendly, encouraging feedback.

            Respond in this exact JSON format only:
            {
              "correct": true/false,
              "feedback": "Brief evaluation of their answer",
              "suggestion": "If incorrect, suggest how to improve (null if correct)"
            }
        """.trimIndent()
    }

    private fun generateFeedback(prompt: String): AnswerFeedback {
        try {
            val request = OllamaRequest(
                model = model,
                prompt = prompt,
                stream = false,
                format = "json"
            )

            val httpRequest = HttpRequest.POST("/api/generate", request)
            val response = httpClient.toBlocking().retrieve(httpRequest, OllamaResponse::class.java)

            return parseFeedbackResponse(response.response)
        } catch (e: Exception) {
            logger.error("Failed to generate feedback from Ollama", e)
            return AnswerFeedback(
                correct = false,
                feedback = "Unable to check answer at this time.",
                suggestion = "Please try again later."
            )
        }
    }

    private fun parseFeedbackResponse(response: String): AnswerFeedback {
        return try {
            objectMapper.readValue(response)
        } catch (e: Exception) {
            logger.error("Failed to parse feedback response: $response", e)
            AnswerFeedback(
                correct = false,
                feedback = "Error processing feedback",
                suggestion = null
            )
        }
    }
}
