package com.jpy.wordbook.service

import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@Singleton
class AudioService(
    @Value("\${audio.cache-dir}") private val cacheDir: String,
    @Value("\${audio.voice}") private val voice: String
) {
    private val logger = LoggerFactory.getLogger(AudioService::class.java)
    private val audioCachePath = File(cacheDir)

    init {
        if (!audioCachePath.exists()) {
            audioCachePath.mkdirs()
            logger.info("Created audio cache directory: $cacheDir")
        }
    }

    fun getOrGenerateAudio(japaneseText: String): String? {
        val hash = generateHash(japaneseText)
        val audioFile = File(audioCachePath, "$hash.mp3")

        if (audioFile.exists()) {
            logger.debug("Audio cache hit for hash: $hash")
            return hash
        }

        return try {
            generateAudio(japaneseText, audioFile)
            hash
        } catch (e: Exception) {
            logger.error("Failed to generate audio for text: $japaneseText", e)
            null
        }
    }

    fun getAudioFile(hash: String): File? {
        val audioFile = File(audioCachePath, "$hash.mp3")
        return if (audioFile.exists()) audioFile else null
    }

    private fun generateHash(text: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray())
            .take(16)
            .joinToString("") { "%02x".format(it) }
    }

    private fun generateAudio(text: String, outputFile: File) {
        logger.info("Generating audio for text (hash: ${outputFile.nameWithoutExtension})")

        val scriptPath = File("scripts/tts_generator.py").absolutePath
        val venvPython = File(".venv/bin/python3").absolutePath

        // Use venv python if available, otherwise fall back to system python3
        val pythonPath = if (File(venvPython).exists()) venvPython else "python3"

        val process = ProcessBuilder(
            pythonPath, scriptPath,
            "--text", text,
            "--voice", voice,
            "--output", outputFile.absolutePath
        )
            .redirectErrorStream(true)
            .start()

        val completed = process.waitFor(60, TimeUnit.SECONDS)

        if (!completed) {
            process.destroyForcibly()
            throw RuntimeException("TTS generation timed out")
        }

        if (process.exitValue() != 0) {
            val output = process.inputStream.bufferedReader().readText()
            throw RuntimeException("TTS generation failed: $output")
        }

        logger.info("Audio generated successfully: ${outputFile.name}")
    }
}
