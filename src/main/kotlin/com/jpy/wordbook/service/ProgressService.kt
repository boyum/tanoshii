package com.jpy.wordbook.service

import com.jpy.wordbook.repository.ProgressRepository
import com.jpy.wordbook.repository.WordRepository
import jakarta.inject.Singleton

data class ProgressStats(
    val totalWords: Long,
    val wordsSeen: Long,
    val totalPractices: Long,
    val completedSessions: Long,
    val progressPercentage: Int
)

@Singleton
class ProgressService(
    private val progressRepository: ProgressRepository,
    private val wordRepository: WordRepository
) {

    fun recordWordsSeen(wordIds: List<Long>) {
        progressRepository.recordWordsSeen(wordIds)
    }

    fun getStats(): ProgressStats {
        val totalWords = wordRepository.count()
        val wordsSeen = progressRepository.getTotalWordsSeen()
        val totalPractices = progressRepository.getTotalPracticeCount()
        val completedSessions = progressRepository.getCompletedSessionsCount()
        val progressPercentage = if (totalWords > 0) {
            ((wordsSeen * 100) / totalWords).toInt()
        } else 0

        return ProgressStats(
            totalWords = totalWords,
            wordsSeen = wordsSeen,
            totalPractices = totalPractices,
            completedSessions = completedSessions,
            progressPercentage = progressPercentage
        )
    }
}
