package com.jpy.wordbook.service

import com.jpy.wordbook.repository.MasteryStats
import com.jpy.wordbook.repository.ProgressRepository
import com.jpy.wordbook.repository.WordRepository
import jakarta.inject.Singleton

data class ProgressStats(
    val totalWords: Long,
    val wordsSeen: Long,
    val totalPractices: Long,
    val completedSessions: Long,
    val progressPercentage: Int,
    val masteryStats: MasteryStats
) {
    val masteredPercentage: Int
        get() = if (totalWords > 0) {
            ((masteryStats.masteredCount * 100) / totalWords).toInt()
        } else 0
}

@Singleton
class ProgressService(
    private val progressRepository: ProgressRepository,
    private val wordRepository: WordRepository,
    private val xpService: XpService
) {

    fun recordWordsSeen(wordIds: List<Long>) {
        // Check which words are new (first encounter)
        val newWordIds = wordIds.filter { wordId ->
            val progress = progressRepository.getProgress(wordId)
            progress == null || progress.timesSeen == 0
        }

        // Record all words
        progressRepository.recordWordsSeen(wordIds)

        // Award XP for new words
        if (newWordIds.isNotEmpty()) {
            val newWords = wordRepository.findByIds(newWordIds)
            newWords.forEach { word ->
                word.id?.let { wordId ->
                    xpService.awardNewWordXp(wordId, word.japanese)
                }
            }
        }
    }

    fun getStats(): ProgressStats {
        val totalWords = wordRepository.count()
        val wordsSeen = progressRepository.getTotalWordsSeen()
        val totalPractices = progressRepository.getTotalPracticeCount()
        val completedSessions = progressRepository.getCompletedSessionsCount()
        val masteryStats = progressRepository.getWordMasteryStats()

        val progressPercentage = if (totalWords > 0) {
            ((wordsSeen * 100) / totalWords).toInt()
        } else 0

        return ProgressStats(
            totalWords = totalWords,
            wordsSeen = wordsSeen,
            totalPractices = totalPractices,
            completedSessions = completedSessions,
            progressPercentage = progressPercentage,
            masteryStats = masteryStats
        )
    }
}
