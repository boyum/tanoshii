package com.jpy.wordbook.service

import com.jpy.wordbook.model.*
import com.jpy.wordbook.repository.TaskRepository
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
class TaskGeneratorService(
    private val vocabularyService: VocabularyService,
    private val ollamaService: OllamaService,
    private val japaneseTextService: JapaneseTextService,
    private val audioService: AudioService,
    private val taskRepository: TaskRepository
) {
    private val logger = LoggerFactory.getLogger(TaskGeneratorService::class.java)

    fun generateTasksForSession(session: Session): List<Task> {
        logger.info("Generating tasks for session ${session.id} with difficulty ${session.difficulty}")

        val taskConfigs = getTaskConfigsForDifficulty(session.difficulty)
        val tasks = mutableListOf<Task>()

        for ((index, config) in taskConfigs.withIndex()) {
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

            val savedTask = taskRepository.save(task)
            tasks.add(savedTask)
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
                // 5 sentences + 5 stories
                (0 until 5).map { TaskConfig(TaskType.SENTENCE, 2) } +
                (0 until 5).map { TaskConfig(TaskType.STORY, 3) }
            }
            Difficulty.HARD -> {
                // 10 longer sentences
                (0 until 10).map { TaskConfig(TaskType.SENTENCE, 3) }
            }
        }
    }

    private data class TaskConfig(val type: TaskType, val wordCount: Int)
}
