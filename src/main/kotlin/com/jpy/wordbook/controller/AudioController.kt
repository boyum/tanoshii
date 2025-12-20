package com.jpy.wordbook.controller

import com.jpy.wordbook.repository.TaskRepository
import com.jpy.wordbook.service.AudioService
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.server.types.files.StreamedFile
import java.io.FileInputStream

@Controller
class AudioController(
    private val audioService: AudioService,
    private val taskRepository: TaskRepository
) {

    @Get("/audio/{hash}.mp3")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun getAudio(@PathVariable hash: String): HttpResponse<StreamedFile> {
        val audioFile = audioService.getAudioFile(hash)
            ?: return HttpResponse.notFound()

        return HttpResponse.ok(StreamedFile(FileInputStream(audioFile), MediaType.of("audio/mpeg")))
            .header("Content-Disposition", "inline; filename=\"$hash.mp3\"")
            .header("Cache-Control", "public, max-age=31536000")
    }

    @Post("/api/session/{sessionId}/task/{taskIndex}/audio")
    fun generateAudio(
        @PathVariable sessionId: String,
        @PathVariable taskIndex: Int
    ): HttpResponse<Map<String, String?>> {
        val task = taskRepository.findBySessionIdAndIndex(sessionId, taskIndex)
            ?: return HttpResponse.notFound()

        // Generate audio if not already cached
        val hash = if (task.audioHash != null) {
            task.audioHash
        } else {
            val newHash = audioService.getOrGenerateAudio(task.japaneseText)
            if (newHash != null && task.id != null) {
                taskRepository.updateAudioHash(task.id, newHash)
            }
            newHash
        }

        return HttpResponse.ok(mapOf("audioHash" to hash))
    }
}
