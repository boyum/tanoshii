package com.jpy.wordbook.service

import com.jpy.wordbook.model.*
import com.jpy.wordbook.repository.TaskRepository
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

@Singleton
class TaskGeneratorService(
    private val vocabularyService: VocabularyService,
    private val ollamaService: OllamaService,
    private val japaneseTextService: JapaneseTextService,
    private val audioService: AudioService,
    private val taskRepository: TaskRepository,
    private val sessionRepository: com.jpy.wordbook.repository.SessionRepository
) {
    private val logger = LoggerFactory.getLogger(TaskGeneratorService::class.java)

    // Track total expected tasks per session for progress reporting
    private val sessionTotalTasks = ConcurrentHashMap<String, Int>()

    fun getTotalTasksForSession(sessionId: String): Int {
        // Check in-memory cache first
        sessionTotalTasks[sessionId]?.let { return it }

        // If not in cache, look up session and calculate from difficulty
        val session = sessionRepository.findById(sessionId) ?: return 0
        val taskConfigs = getTaskConfigsForDifficulty(session.difficulty)
        val total = taskConfigs.size

        // Cache it for future calls
        sessionTotalTasks[sessionId] = total

        return total
    }

    fun getGeneratedTaskCount(sessionId: String): Int {
        return taskRepository.countBySessionId(sessionId)
    }

    /**
     * Generate the first task for a session and return immediately.
     * Call generateRemainingTasks() afterwards in a background thread.
     */
    fun generateFirstTask(session: Session): Task {
        logger.info("Generating first task for session ${session.id}")

        val taskConfigs = getTaskConfigsForDifficulty(session.difficulty)
        sessionTotalTasks[session.id] = taskConfigs.size

        val config = taskConfigs.first()
        return generateSingleTask(session, 0, config)
    }

    /**
     * Generate remaining tasks (index 1 onwards) for a session.
     * Should be called in a background thread after generateFirstTask.
     */
    fun generateRemainingTasks(session: Session) {
        logger.info("Generating remaining tasks for session ${session.id}")

        val taskConfigs = getTaskConfigsForDifficulty(session.difficulty)

        for ((index, config) in taskConfigs.withIndex()) {
            if (index == 0) continue // Skip first task, already generated

            generateSingleTask(session, index, config)
            logger.info("Generated task ${index + 1}/${taskConfigs.size} for session ${session.id}")
        }

        logger.info("Completed all tasks for session ${session.id}")
    }

    private fun generateSingleTask(session: Session, index: Int, config: TaskConfig): Task {
        val words = vocabularyService.getRandomWords(config.wordCount)
        val content = generateContent(words, config.type, session.difficulty)

        // Generate furigana and romaji using Kuromoji
        val furigana = japaneseTextService.toFurigana(content.japanese)
        val romaji = japaneseTextService.toRomaji(content.japanese)

        // Generate audio for this task
        val audioHash = audioService.getOrGenerateAudio(content.japanese)

        val task = Task(
            sessionId = session.id,
            taskIndex = index,
            taskType = config.type,
            japaneseText = content.japanese,
            englishTranslation = content.english,
            wordIds = words.mapNotNull { it.id },
            audioHash = audioHash,
            furiganaText = furigana,
            romajiText = romaji
        )

        return taskRepository.save(task)
    }

    fun generateTasksForSession(session: Session): List<Task> {
        logger.info("Generating tasks for session ${session.id} with difficulty ${session.difficulty}")

        val taskConfigs = getTaskConfigsForDifficulty(session.difficulty)
        sessionTotalTasks[session.id] = taskConfigs.size
        val tasks = mutableListOf<Task>()

        for ((index, config) in taskConfigs.withIndex()) {
            val task = generateSingleTask(session, index, config)
            tasks.add(task)
            logger.info("Generated task ${index + 1}/${taskConfigs.size} for session ${session.id}")
        }

        return tasks
    }

    private fun generateContent(words: List<Word>, type: TaskType, difficulty: Difficulty): TaskContent {
        return when (type) {
            TaskType.SENTENCE -> {
                if (difficulty == Difficulty.HARD) {
                    ollamaService.generateLongerSentence(words)
                } else {
                    ollamaService.generateSentence(words)
                }
            }
            TaskType.STORY -> ollamaService.generateStory(words)
        }
    }

    private fun getTaskConfigsForDifficulty(difficulty: Difficulty): List<TaskConfig> {
        return when (difficulty) {
            Difficulty.EASY -> {
                // 10 short sentences
                (0 until 10).map { TaskConfig(TaskType.SENTENCE, 2) }
            }
            Difficulty.MEDIUM -> {
                // 3 sentences + 7 stories
                (0 until 3).map { TaskConfig(TaskType.SENTENCE, 2) } +
                (0 until 7).map { TaskConfig(TaskType.STORY, 3) }
            }
            Difficulty.HARD -> {
                // 10 stories
                (0 until 10).map { TaskConfig(TaskType.STORY, 4) }
            }
        }
    }

    private data class TaskConfig(val type: TaskType, val wordCount: Int)
}
