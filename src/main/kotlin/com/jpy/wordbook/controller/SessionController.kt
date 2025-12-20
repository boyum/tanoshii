package com.jpy.wordbook.controller

import com.jpy.wordbook.model.Difficulty
import com.jpy.wordbook.model.Session
import com.jpy.wordbook.model.Word
import com.jpy.wordbook.repository.SessionRepository
import com.jpy.wordbook.repository.TaskRepository
import com.jpy.wordbook.service.TaskGeneratorService
import com.jpy.wordbook.service.VocabularyService
import com.jpy.wordbook.service.OllamaService
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.views.View
import java.util.UUID
import java.util.concurrent.ExecutorService
import jakarta.inject.Named

@Controller
class SessionController(
    private val sessionRepository: SessionRepository,
    private val taskRepository: TaskRepository,
    private val taskGeneratorService: TaskGeneratorService,
    private val vocabularyService: VocabularyService,
    private val ollamaService: OllamaService,
    private val japaneseTextService: com.jpy.wordbook.service.JapaneseTextService,
    @Named(TaskExecutors.IO) private val executorService: ExecutorService
) {

    @Post("/api/session/start")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @ExecuteOn(TaskExecutors.BLOCKING)
    fun startSession(difficulty: String?): HttpResponse<*> {
        val difficultyEnum = Difficulty.valueOf((difficulty ?: "easy").uppercase())

        val sessionId = UUID.randomUUID().toString()
        val session = Session(id = sessionId, difficulty = difficultyEnum)
        sessionRepository.save(session)

        // Generate first task synchronously (user sees it immediately)
        taskGeneratorService.generateFirstTask(session)

        // Generate remaining tasks in background
        executorService.submit {
            taskGeneratorService.generateRemainingTasks(session)
        }

        return HttpResponse.ok<Unit>()
            .header("HX-Redirect", "/session/$sessionId")
    }

    @Get("/api/session/{id}/status")
    fun getSessionStatus(@PathVariable id: String): Map<String, Any> {
        val generated = taskGeneratorService.getGeneratedTaskCount(id)
        val total = taskGeneratorService.getTotalTasksForSession(id)
        return mapOf(
            "generated" to generated,
            "total" to total,
            "complete" to (generated >= total && total > 0)
        )
    }

    @Get("/session/{id}")
    @View("session")
    fun getSession(@PathVariable id: String): Map<String, Any?> {
        val session = sessionRepository.findById(id)
            ?: return mapOf("error" to "Session not found")

        val tasks = taskRepository.findBySessionId(id)
        val currentTask = tasks.getOrNull(session.currentTaskIndex)

        return mapOf(
            "title" to "Session - Tanoshii",
            "session" to session,
            "tasks" to tasks,
            "currentTask" to currentTask,
            "currentIndex" to session.currentTaskIndex,
            "totalTasks" to tasks.size,
            "words" to (currentTask?.let { vocabularyService.getWordsByIds(it.wordIds) } ?: emptyList<Word>())
        )
    }

    @Get("/api/session/{id}/task/{index}")
    @View("fragments/task-content")
    fun getTask(@PathVariable id: String, @PathVariable index: Int): Map<String, Any?> {
        val session = sessionRepository.findById(id)
            ?: return mapOf("error" to "Session not found")

        sessionRepository.updateCurrentIndex(id, index)

        val task = taskRepository.findBySessionIdAndIndex(id, index)
            ?: return mapOf("error" to "Task not found")

        val words = vocabularyService.getWordsByIds(task.wordIds)
        val tasks = taskRepository.findBySessionId(id)

        // Create vocabulary map (Japanese -> English) for hover tooltips
        // Load ALL vocabulary so every word can show its meaning
        val vocabularyMap = vocabularyService.getAllWords().associate { it.japanese to it.english }

        // Split into sentences with word tokens for word-by-word romaji display
        val sentencesWithTokens = japaneseTextService.toSentencesWithTokens(task.japaneseText, vocabularyMap)

        return mapOf(
            "sessionId" to id,
            "task" to task,
            "taskIndex" to index,
            "totalTasks" to tasks.size,
            "words" to words,
            "showTranslation" to false,
            "sentencesWithTokens" to sentencesWithTokens
        )
    }

    @Post("/api/session/{id}/task/{index}/reveal")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED, MediaType.ALL)
    @View("fragments/translation")
    fun revealTranslation(@PathVariable id: String, @PathVariable index: Int): Map<String, Any?> {
        val task = taskRepository.findBySessionIdAndIndex(id, index)
            ?: return mapOf("error" to "Task not found")

        return mapOf(
            "translation" to task.englishTranslation
        )
    }

    @Post("/api/session/{id}/task/{index}/check")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED, MediaType.MULTIPART_FORM_DATA)
    @ExecuteOn(TaskExecutors.BLOCKING)
    @View("fragments/feedback")
    fun checkAnswer(
        @PathVariable id: String,
        @PathVariable index: Int,
        userAnswer: String
    ): Map<String, Any?> {
        val task = taskRepository.findBySessionIdAndIndex(id, index)
            ?: return mapOf("error" to "Task not found")

        val feedback = ollamaService.checkAnswer(
            japaneseText = task.japaneseText,
            correctTranslation = task.englishTranslation,
            userAnswer = userAnswer
        )

        return mapOf(
            "correct" to feedback.correct,
            "feedback" to feedback.feedback,
            "suggestion" to feedback.suggestion
        )
    }
}
