package com.jpy.wordbook.service

import com.jpy.wordbook.model.*
import com.jpy.wordbook.repository.TaskRepository
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

@Singleton
class TaskGeneratorService(
    private val vocabularyService: VocabularyService,
    private val ollamaService: OllamaService,
    private val japaneseTextService: JapaneseTextService,
    private val audioService: AudioService,
    private val taskRepository: TaskRepository,
    private val sessionRepository: com.jpy.wordbook.repository.SessionRepository,
    @Value("\${llm.provider:ollama}") private val provider: String
) {
    private val logger = LoggerFactory.getLogger(TaskGeneratorService::class.java)

    // Track total expected tasks per session for progress reporting
    private val sessionTotalTasks = ConcurrentHashMap<String, Int>()

    // Track sessions that are currently generating tasks to prevent race conditions
    private val generatingLocks = ConcurrentHashMap<String, Any>()

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
     * Generate the next batch of tasks for a session.
     * Should be called in a background thread after generateFirstTask.
     * For Ollama: Uses parallel generation with coroutines for 6-10x speedup.
     * For Gemini: Uses sequential generation to avoid rate limiting.
     *
     * Thread-safe: Uses per-session locks to prevent race conditions.
     *
     * @param session The session to generate tasks for
     * @param startIndex The index to start generating from (inclusive)
     * @param count The number of tasks to generate (default 5)
     */
    fun generateNextBatch(session: Session, startIndex: Int, count: Int = 5) {
        // Get or create a lock for this session
        val lock = generatingLocks.computeIfAbsent(session.id) { Any() }

        // Synchronize on the session-specific lock to prevent concurrent batch generation
        synchronized(lock) {
            logger.info("Generating tasks ${startIndex + 1} to ${startIndex + count} for session ${session.id}")

            val taskConfigs = getTaskConfigsForDifficulty(session.difficulty)
            val endIndex = minOf(startIndex + count, taskConfigs.size)

            if (provider == "gemini") {
                // Sequential generation for Gemini to avoid rate limiting
                logger.info("Using sequential generation for Gemini")
                for (index in startIndex until endIndex) {
                    val config = taskConfigs[index]
                    try {
                        generateSingleTask(session, index, config)
                        logger.info("Generated task ${index + 1}/${taskConfigs.size} for session ${session.id}")
                    } catch (e: Exception) {
                        logger.error("Failed to generate task ${index + 1} for session ${session.id}", e)
                    }
                }
            } else {
                // Parallel generation for Ollama (faster)
                logger.info("Using parallel generation for Ollama")
                runBlocking {
                    val deferredTasks = (startIndex until endIndex).map { index ->
                        val config = taskConfigs[index]
                        async(Dispatchers.IO) {
                            try {
                                generateSingleTask(session, index, config)
                                logger.info("Generated task ${index + 1}/${taskConfigs.size} for session ${session.id}")
                            } catch (e: Exception) {
                                logger.error("Failed to generate task ${index + 1} for session ${session.id}", e)
                            }
                        }
                    }
                    deferredTasks.awaitAll()
                }
            }

            logger.info("Completed batch for session ${session.id}")
        }
    }

    /**
     * Generate remaining tasks (index 1 onwards) for a session in parallel.
     * Now generates only the next 5 tasks instead of all remaining tasks.
     * Should be called in a background thread after generateFirstTask.
     */
    fun generateRemainingTasks(session: Session) {
        generateNextBatch(session, startIndex = 1, count = 5)
    }

    private fun generateSingleTask(session: Session, index: Int, config: TaskConfig): Task {
        // Check if task already exists (prevent race condition duplicates)
        val existingTask = taskRepository.findBySessionIdAndIndex(session.id, index)
        if (existingTask != null) {
            logger.debug("Task $index for session ${session.id} already exists, skipping generation")
            return existingTask
        }

        val words = vocabularyService.getRandomWordsByDifficulty(config.wordCount, session.difficulty)
        val content = generateContent(words, config.type, session.difficulty, session.topic)

        // Generate furigana and romaji using Kuromoji
        val furigana = japaneseTextService.toFurigana(content.japanese)
        val romaji = japaneseTextService.toRomaji(content.japanese)

        // Generate audio for this task
        val audioHash = audioService.getOrGenerateAudio(content.japanese)

        // Word translations now come from the same LLM call (content.wordTranslations)
        // Fallback to separate call only if not provided
        val wordTranslations = content.wordTranslations
            ?: ollamaService.generateWordTranslations(content.japanese)

        val task = Task(
            sessionId = session.id,
            taskIndex = index,
            taskType = config.type,
            japaneseText = content.japanese,
            englishTranslation = content.english,
            wordIds = words.mapNotNull { it.id },
            audioHash = audioHash,
            furiganaText = furigana,
            romajiText = romaji,
            wordTranslations = wordTranslations
        )

        return taskRepository.save(task)
    }

    private fun generateContent(words: List<Word>, type: TaskType, difficulty: Difficulty, topic: String?): TaskContent {
        return when (type) {
            TaskType.SENTENCE -> {
                if (difficulty == Difficulty.HARD) {
                    ollamaService.generateLongerSentence(words, topic)
                } else {
                    ollamaService.generateSentence(words, topic)
                }
            }
            TaskType.STORY -> ollamaService.generateStory(words, topic)
        }
    }

    private fun getTaskConfigsForDifficulty(difficulty: Difficulty): List<TaskConfig> {
        return when (difficulty) {
            Difficulty.EASY -> {
                // 100 short sentences
                (0 until 100).map { TaskConfig(TaskType.SENTENCE, 2) }
            }
            Difficulty.MEDIUM -> {
                // 30 sentences + 70 stories
                (0 until 30).map { TaskConfig(TaskType.SENTENCE, 2) } +
                (0 until 70).map { TaskConfig(TaskType.STORY, 3) }
            }
            Difficulty.HARD -> {
                // 100 stories
                (0 until 100).map { TaskConfig(TaskType.STORY, 4) }
            }
        }
    }

    private data class TaskConfig(val type: TaskType, val wordCount: Int)
}
