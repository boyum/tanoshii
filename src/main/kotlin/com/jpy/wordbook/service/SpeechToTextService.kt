package com.jpy.wordbook.service

import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@Singleton
class SpeechToTextService {
    private val logger = LoggerFactory.getLogger(SpeechToTextService::class.java)
    private val tempDir = Files.createTempDirectory("stt")
    private val projectDir = System.getProperty("user.dir")
    private val scriptPath = findScriptPath()
    private val pythonPath = findPythonPath()

    private fun findScriptPath(): String {
        val possiblePaths = listOf(
            "$projectDir/scripts/stt_service.py",
            "scripts/stt_service.py"
        )

        for (path in possiblePaths) {
            if (File(path).exists()) {
                return File(path).absolutePath
            }
        }

        return "$projectDir/scripts/stt_service.py"
    }

    private fun findPythonPath(): String {
        // Prefer virtual environment Python if it exists
        val venvPython = "$projectDir/.venv/bin/python3"
        if (File(venvPython).exists()) {
            return venvPython
        }
        return "python3"
    }

    fun transcribe(audioData: ByteArray, format: String = "webm"): String {
        // Save audio to temp file
        val tempFile = Files.createTempFile(tempDir, "audio_", ".$format")
        Files.write(tempFile, audioData)

        try {
            return runWhisper(tempFile)
        } finally {
            // Clean up temp file
            try {
                Files.deleteIfExists(tempFile)
            } catch (e: Exception) {
                logger.warn("Failed to delete temp file: $tempFile", e)
            }
        }
    }

    private fun runWhisper(audioPath: Path): String {
        logger.info("Running Whisper with python: $pythonPath, script: $scriptPath")
        val process = ProcessBuilder(
            pythonPath, scriptPath, audioPath.toString()
        )
            .redirectErrorStream(false)
            .start()

        val completed = process.waitFor(60, TimeUnit.SECONDS)

        if (!completed) {
            process.destroyForcibly()
            throw RuntimeException("Whisper transcription timed out")
        }

        val exitCode = process.exitValue()
        val output = process.inputStream.bufferedReader().readText().trim()
        val error = process.errorStream.bufferedReader().readText().trim()

        if (exitCode != 0) {
            logger.error("Whisper failed with exit code $exitCode: $error")
            throw RuntimeException("Whisper transcription failed: $error")
        }

        logger.info("Transcribed: $output")
        return output
    }
}
