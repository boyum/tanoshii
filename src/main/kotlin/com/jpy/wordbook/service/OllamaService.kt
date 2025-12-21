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
    val stream: Boolean = false
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

    fun generateSentence(words: List<Word>, topic: String? = null): TaskContent {
        val prompt = buildSentencePrompt(words, topic)
        return generate(prompt)
    }

    fun generateLongerSentence(words: List<Word>, topic: String? = null): TaskContent {
        val prompt = buildLongerSentencePrompt(words, topic)
        return generate(prompt)
    }

    fun generateStory(words: List<Word>, topic: String? = null): TaskContent {
        val prompt = buildStoryPrompt(words, topic)
        return generate(prompt)
    }

    fun checkAnswer(japaneseText: String, correctTranslation: String, userAnswer: String): AnswerFeedback {
        val prompt = buildCheckAnswerPrompt(japaneseText, correctTranslation, userAnswer)
        return generateFeedback(prompt)
    }

    fun generateWordTranslations(japaneseText: String): Map<String, String> {
        val prompt = buildWordTranslationsPrompt(japaneseText)
        return generateTranslations(prompt)
    }

    fun generateTopicSuggestions(): List<String> {
        val prompt = buildTopicSuggestionsPrompt()
        return generateTopics(prompt)
    }

    private fun buildSentencePrompt(words: List<Word>, topic: String?): String {
        val wordList = words.joinToString(", ") { it.japanese }
        val topicContext = topic?.let { "\n            - The sentence should be about or related to: $it" } ?: ""
        return """
            Create a natural Japanese sentence that MUST include this word: ${words.first().japanese}

            Guidelines:
            - Write a simple, natural sentence (beginner level)
            - The word "$wordList" MUST appear in the sentence exactly as written$topicContext
            - You may add common words (particles, verbs, adjectives) to make the sentence grammatically correct
            - Use appropriate kanji with hiragana/katakana
            - Aim for 5-15 characters
            - End with ONLY ONE punctuation mark (。 or ！ or ？, never multiple)

            Respond in this exact JSON format only:
            {"japanese": "日本語の文", "english": "English translation"}
        """.trimIndent()
    }

    private fun buildLongerSentencePrompt(words: List<Word>, topic: String?): String {
        val wordList = words.joinToString(", ") { it.japanese }
        val topicContext = topic?.let { "\n            - The sentence should be about or related to: $it" } ?: ""
        return """
            Create a natural Japanese sentence that MUST include at least one of these words: $wordList

            Guidelines:
            - Write an intermediate level sentence
            - At least one word from the list MUST appear exactly as written$topicContext
            - You may add common vocabulary to make it natural and meaningful
            - Use appropriate kanji, hiragana, and katakana
            - Aim for 12-25 characters
            - Make it interesting or useful for daily life
            - End with ONLY ONE punctuation mark (。 or ！ or ？, never multiple)

            Respond in this exact JSON format only:
            {"japanese": "日本語の文", "english": "English translation"}
        """.trimIndent()
    }

    private fun buildStoryPrompt(words: List<Word>, topic: String?): String {
        val wordList = words.joinToString(", ") { it.japanese }
        val topicContext = topic?.let { "\n            - The story should be about or related to: $it" } ?: ""
        return """
            Create a short Japanese story using these words: $wordList

            IMPORTANT REQUIREMENTS:
            - The story MUST have at least 3 sentences, preferably 3-4 sentences (use periods 。 to separate them)
            - At least one word from the list MUST appear exactly as written$topicContext
            - Write at beginner to intermediate level
            - Use appropriate kanji, hiragana, and katakana
            - Make the sentences form a coherent mini-story with a beginning, middle, and end
            - Each sentence must end with ONLY ONE punctuation mark (。 or ！ or ？, never multiple like ！。)

            Example structure: [Setup sentence]。[Development sentence]。[More development/action]。[Conclusion sentence]。

            Respond in this exact JSON format only:
            {"japanese": "文1。文2。文3。文4。", "english": "English translation of all sentences"}
        """.trimIndent()
    }

    private fun generate(prompt: String): TaskContent {
        try {
            val request = OllamaRequest(
                model = model,
                prompt = prompt,
                stream = false
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
            val content: TaskContent = objectMapper.readValue(response)
            // Clean up punctuation issues in Japanese text
            val cleanedJapanese = cleanupPunctuation(content.japanese)
            content.copy(japanese = cleanedJapanese)
        } catch (e: Exception) {
            logger.error("Failed to parse Ollama response: $response", e)
            TaskContent(
                japanese = "パースエラー",
                english = "Parse error"
            )
        }
    }

    /**
     * Clean up common punctuation issues in Japanese text:
     * - Remove duplicate sentence-ending punctuation (e.g., "！。" -> "！")
     * - Remove extra periods after other punctuation
     */
    private fun cleanupPunctuation(text: String): String {
        return text
            // Remove period after exclamation/question marks
            .replace(Regex("！。+"), "！")
            .replace(Regex("？。+"), "？")
            // Remove duplicate periods
            .replace(Regex("。+"), "。")
            // Remove duplicate exclamation marks
            .replace(Regex("！+"), "！")
            // Remove duplicate question marks
            .replace(Regex("？+"), "？")
            // Remove period before exclamation/question marks
            .replace(Regex("。+([！？])"), "$1")
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
                stream = false
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

    private fun buildWordTranslationsPrompt(japaneseText: String): String {
        return """
            You are a Japanese language expert. Analyze the following Japanese text and provide word-by-word translations.

            Japanese text: $japaneseText

            CRITICAL: Break down the text into INDIVIDUAL TOKENS. Do NOT combine words together.
            - Each kanji/kana character or word should be separate
            - Particles MUST be separate entries (は, が, を, に, で, と, も, の, etc.)
            - Do NOT combine particles with words (e.g., "私は" should be TWO entries: "私" and "は")
            - For verb conjugations, provide the conjugated form as it appears
            - For compound words, if they appear as one unit, keep them together
            - IGNORE punctuation marks (。、！？) - do not include them in the output

            Include translations for:
            - Individual nouns, verbs, adjectives, adverbs
            - Each particle separately with its grammatical function
            - Verb endings and auxiliaries (です, ます, た, etc.)

            Respond in this exact JSON format only:
            {
              "word1": "meaning1",
              "word2": "meaning2",
              "particle": "grammatical function"
            }

            Example for "私は学生です":
            {
              "私": "I, me",
              "は": "topic marker (wa)",
              "学生": "student",
              "です": "to be (polite)"
            }

            Example for "猫が好きです":
            {
              "猫": "cat",
              "が": "subject marker (ga)",
              "好き": "like, favorite",
              "です": "to be (polite)"
            }
        """.trimIndent()
    }

    private fun generateTranslations(prompt: String): Map<String, String> {
        try {
            val request = OllamaRequest(
                model = model,
                prompt = prompt,
                stream = false
            )

            val httpRequest = HttpRequest.POST("/api/generate", request)
            val response = httpClient.toBlocking().retrieve(httpRequest, OllamaResponse::class.java)

            return parseTranslationsResponse(response.response)
        } catch (e: Exception) {
            logger.error("Failed to generate word translations from Ollama", e)
            return emptyMap()
        }
    }

    private fun parseTranslationsResponse(response: String): Map<String, String> {
        return try {
            objectMapper.readValue(response)
        } catch (e: Exception) {
            logger.error("Failed to parse translations response: $response", e)
            emptyMap()
        }
    }

    private fun buildTopicSuggestionsPrompt(): String {
        return """
            Generate 5 random and diverse topics that are interesting and creative.

            Guidelines:
            - Make topics varied and unexpected - don't just stick to typical Japanese learning themes
            - Include completely different categories: nature, technology, emotions, activities, places, objects, etc.
            - Topics can be abstract or concrete
            - Keep topics simple and evocative (2-4 words)
            - Be creative and surprising with your choices

            Respond in this exact JSON format only:
            {"topics": ["topic1", "topic2", "topic3", "topic4", "topic5"]}

            Example:
            {"topics": ["Mountain climbing", "Video games", "Rainy days", "Robot dreams", "Old bookstores"]}
        """.trimIndent()
    }

    private fun generateTopics(prompt: String): List<String> {
        try {
            val request = OllamaRequest(
                model = model,
                prompt = prompt,
                stream = false
            )

            val httpRequest = HttpRequest.POST("/api/generate", request)
            val response = httpClient.toBlocking().retrieve(httpRequest, OllamaResponse::class.java)

            return parseTopicsResponse(response.response)
        } catch (e: Exception) {
            logger.error("Failed to generate topics from Ollama", e)
            // Return random fallback topics
            val allFallbackTopics = listOf(
                // Nature & Weather
                "Starry nights", "Ocean waves", "Thunderstorms", "Spring flowers", "Beach sunsets",
                "Mountain hiking", "Desert landscapes", "Autumn leaves", "Cherry blossoms", "Tropical rainforests",
                "Northern lights", "Full moon", "Rainbow after rain", "Misty mornings", "Snow-covered fields",
                "Bamboo forests", "Coral reefs", "Wildflower meadows", "Volcanic landscapes", "Foggy mountains",

                // Urban Life
                "Coffee shops", "City lights", "Train stations", "Busy mornings", "Rooftop views",
                "Street markets", "Neon signs", "Underground passages", "Rush hour", "Convenience stores",
                "City parks", "Taxi rides", "Pedestrian crossings", "Skyscrapers", "Bicycle lanes",
                "Food courts", "Shopping arcades", "Street performers", "City squares", "Night markets",

                // Activities & Hobbies
                "Video games", "Winter sports", "Martial arts", "Garden parties", "Cooking together",
                "Board games", "Stargazing", "Photography walks", "Pottery making", "Calligraphy practice",
                "Fishing trips", "Rock climbing", "Camping adventures", "Bird watching", "Knitting circles",
                "Dancing lessons", "Karaoke nights", "Origami folding", "Tea ceremonies", "Meditation retreats",

                // Culture & Arts
                "Music festivals", "Art museums", "Ghost stories", "Ancient ruins", "Theater performances",
                "Film festivals", "Poetry readings", "Jazz clubs", "Street art", "Opera houses",
                "Cultural festivals", "Traditional crafts", "Comic books", "Anime conventions", "Folk tales",
                "Contemporary art", "Historical reenactments", "Puppet shows", "Sculpture gardens", "Dance recitals",

                // Places & Locations
                "Silent libraries", "Space exploration", "River boats", "Airport terminals", "Old bookstores",
                "Mountain temples", "Castle ruins", "Hot springs", "Lighthouse keepers", "Harbor views",
                "Zen gardens", "Village festivals", "Island getaways", "Country roads", "Old neighborhoods",
                "Seaside towns", "Mountain cabins", "Forest shrines", "Underground caves", "Observatory decks",

                // Daily Life & Moments
                "Childhood memories", "Morning routines", "Evening walks", "Weekend plans", "Phone calls",
                "Lost keys", "First dates", "Job interviews", "Moving day", "Birthday surprises",
                "Study sessions", "Late-night snacks", "Pet adventures", "Family dinners", "Road trips",
                "Lazy Sundays", "Power outages", "Spring cleaning", "New Year's resolutions", "Grocery shopping",

                // Abstract & Emotional
                "Distant memories", "Future dreams", "Parallel universes", "Time travel", "Silent wishes",
                "Hidden talents", "Secret gardens", "Forgotten songs", "Unsent letters", "Stolen moments",
                "Bittersweet goodbyes", "Second chances", "Broken promises", "Lost friendships", "New beginnings",
                "Inner peace", "Growing pains", "Cultural identity", "Generation gaps", "Life lessons",

                // Food & Drink
                "Ramen shops", "Tea time", "Street food", "Farmers markets", "Bakery mornings",
                "Sushi bars", "Izakaya nights", "Picnic lunches", "Wine tasting", "Home cooking",
                "Food trucks", "Dessert cafes", "Breakfast traditions", "Dinner parties", "Cooking disasters"
            )
            return allFallbackTopics.shuffled().take(5)
        }
    }

    private fun parseTopicsResponse(response: String): List<String> {
        return try {
            val result: Map<String, List<String>> = objectMapper.readValue(response)
            result["topics"] ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to parse topics response: $response", e)
            // Return random fallback topics (same as above in generateTopicSuggestions)
            val allFallbackTopics = listOf(
                // Nature & Weather
                "Starry nights", "Ocean waves", "Thunderstorms", "Spring flowers", "Beach sunsets",
                "Mountain hiking", "Desert landscapes", "Autumn leaves", "Cherry blossoms", "Tropical rainforests",
                "Northern lights", "Full moon", "Rainbow after rain", "Misty mornings", "Snow-covered fields",
                "Bamboo forests", "Coral reefs", "Wildflower meadows", "Volcanic landscapes", "Foggy mountains",

                // Urban Life
                "Coffee shops", "City lights", "Train stations", "Busy mornings", "Rooftop views",
                "Street markets", "Neon signs", "Underground passages", "Rush hour", "Convenience stores",
                "City parks", "Taxi rides", "Pedestrian crossings", "Skyscrapers", "Bicycle lanes",
                "Food courts", "Shopping arcades", "Street performers", "City squares", "Night markets",

                // Activities & Hobbies
                "Video games", "Winter sports", "Martial arts", "Garden parties", "Cooking together",
                "Board games", "Stargazing", "Photography walks", "Pottery making", "Calligraphy practice",
                "Fishing trips", "Rock climbing", "Camping adventures", "Bird watching", "Knitting circles",
                "Dancing lessons", "Karaoke nights", "Origami folding", "Tea ceremonies", "Meditation retreats",

                // Culture & Arts
                "Music festivals", "Art museums", "Ghost stories", "Ancient ruins", "Theater performances",
                "Film festivals", "Poetry readings", "Jazz clubs", "Street art", "Opera houses",
                "Cultural festivals", "Traditional crafts", "Comic books", "Anime conventions", "Folk tales",
                "Contemporary art", "Historical reenactments", "Puppet shows", "Sculpture gardens", "Dance recitals",

                // Places & Locations
                "Silent libraries", "Space exploration", "River boats", "Airport terminals", "Old bookstores",
                "Mountain temples", "Castle ruins", "Hot springs", "Lighthouse keepers", "Harbor views",
                "Zen gardens", "Village festivals", "Island getaways", "Country roads", "Old neighborhoods",
                "Seaside towns", "Mountain cabins", "Forest shrines", "Underground caves", "Observatory decks",

                // Daily Life & Moments
                "Childhood memories", "Morning routines", "Evening walks", "Weekend plans", "Phone calls",
                "Lost keys", "First dates", "Job interviews", "Moving day", "Birthday surprises",
                "Study sessions", "Late-night snacks", "Pet adventures", "Family dinners", "Road trips",
                "Lazy Sundays", "Power outages", "Spring cleaning", "New Year's resolutions", "Grocery shopping",

                // Abstract & Emotional
                "Distant memories", "Future dreams", "Parallel universes", "Time travel", "Silent wishes",
                "Hidden talents", "Secret gardens", "Forgotten songs", "Unsent letters", "Stolen moments",
                "Bittersweet goodbyes", "Second chances", "Broken promises", "Lost friendships", "New beginnings",
                "Inner peace", "Growing pains", "Cultural identity", "Generation gaps", "Life lessons",

                // Food & Drink
                "Ramen shops", "Tea time", "Street food", "Farmers markets", "Bakery mornings",
                "Sushi bars", "Izakaya nights", "Picnic lunches", "Wine tasting", "Home cooking",
                "Food trucks", "Dessert cafes", "Breakfast traditions", "Dinner parties", "Cooking disasters"
            )
            allFallbackTopics.shuffled().take(5)
        }
    }
}
