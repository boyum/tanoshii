package com.jpy.wordbook.controller

import com.jpy.wordbook.service.AudioService
import com.jpy.wordbook.service.ConversationService
import com.jpy.wordbook.service.SpeechToTextService
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.views.View
import org.slf4j.LoggerFactory

data class StartConversationRequest(
    val scenario: String = "free"
)

data class StartConversationResponse(
    val conversationId: String,
    val greeting: GreetingResponse
)

data class GreetingResponse(
    val japanese: String,
    val english: String,
    val audioHash: String?
)

data class MessageResponse(
    val userText: String?,
    val response: ResponseContent?
)

data class ResponseContent(
    val japanese: String,
    val english: String,
    val audioHash: String?
)

data class TtsRequest(
    val text: String
)

data class TtsResponse(
    val audioHash: String?
)

@Controller
class ConversationController(
    private val conversationService: ConversationService,
    private val speechToTextService: SpeechToTextService,
    private val audioService: AudioService
) {
    private val logger = LoggerFactory.getLogger(ConversationController::class.java)

    @Get("/conversation")
    @View("conversation")
    fun conversationPage(): Map<String, Any> {
        return mapOf("title" to "Conversation - Tanoshii")
    }

    @Post("/api/conversation/start")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ExecuteOn(TaskExecutors.BLOCKING)
    fun startConversation(@Body request: StartConversationRequest): HttpResponse<StartConversationResponse> {
        val (conversationId, greeting) = conversationService.startConversation(request.scenario)

        // Generate audio for greeting
        val audioHash = audioService.getOrGenerateAudio(greeting.japanese)

        return HttpResponse.ok(StartConversationResponse(
            conversationId = conversationId,
            greeting = GreetingResponse(
                japanese = greeting.japanese,
                english = greeting.english,
                audioHash = audioHash
            )
        ))
    }

    @Post("/api/conversation/message")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @ExecuteOn(TaskExecutors.BLOCKING)
    fun processMessage(
        audio: CompletedFileUpload,
        conversationId: String
    ): HttpResponse<MessageResponse> {
        try {
            // Step 1: Transcribe audio to Japanese text
            val audioData = audio.bytes
            val userJapanese = speechToTextService.transcribe(audioData)

            if (userJapanese.isBlank()) {
                return HttpResponse.ok(MessageResponse(
                    userText = null,
                    response = ResponseContent(
                        japanese = "聞こえませんでした。もう一度お願いします。",
                        english = "I couldn't hear you. Please try again.",
                        audioHash = null
                    )
                ))
            }

            logger.info("Transcribed user speech: $userJapanese")

            // Step 2: Generate response from LLM
            val response = conversationService.processUserMessage(conversationId, userJapanese)

            // Step 3: Generate audio for response
            val audioHash = audioService.getOrGenerateAudio(response.japanese)

            return HttpResponse.ok(MessageResponse(
                userText = userJapanese,
                response = ResponseContent(
                    japanese = response.japanese,
                    english = response.english,
                    audioHash = audioHash
                )
            ))
        } catch (e: Exception) {
            logger.error("Failed to process conversation message", e)
            return HttpResponse.ok(MessageResponse(
                userText = null,
                response = ResponseContent(
                    japanese = "すみません、エラーが発生しました。",
                    english = "Sorry, an error occurred.",
                    audioHash = null
                )
            ))
        }
    }

    @Post("/api/conversation/tts")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ExecuteOn(TaskExecutors.BLOCKING)
    fun generateTts(@Body request: TtsRequest): HttpResponse<TtsResponse> {
        val audioHash = audioService.getOrGenerateAudio(request.text)
        return HttpResponse.ok(TtsResponse(audioHash = audioHash))
    }
}
