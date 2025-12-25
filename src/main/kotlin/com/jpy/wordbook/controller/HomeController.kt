package com.jpy.wordbook.controller

import com.jpy.wordbook.repository.SessionRepository
import com.jpy.wordbook.service.ProgressService
import com.jpy.wordbook.service.VocabularyService
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.views.View

@Controller
class HomeController(
    private val vocabularyService: VocabularyService,
    private val sessionRepository: SessionRepository,
    private val progressService: ProgressService,
    private val xpService: com.jpy.wordbook.service.XpService
) {

    @Get("/")
    @View("home")
    fun index(): Map<String, Any> {
        val recentSessions = sessionRepository.findRecent(10)
        val levelInfo = xpService.getLevelInfo()

        return mapOf(
            "title" to "Tanoshii",
            "wordCount" to vocabularyService.getWordCount(),
            "recentSessions" to recentSessions,
            // XP and level data
            "currentLevel" to levelInfo.currentLevel,
            "totalXp" to levelInfo.totalXp,
            "xpToNextLevel" to levelInfo.xpToNextLevel,
            "levelProgress" to levelInfo.progressPercentage
        )
    }

    @Get("/guide")
    @View("guide")
    fun guide(): Map<String, Any> {
        return mapOf(
            "title" to "Learning Guide - Tanoshii"
        )
    }

    @Get("/stats")
    @View("stats")
    fun stats(): Map<String, Any> {
        // Get all sessions (use large limit to get all)
        val sessions = sessionRepository.findRecent(1000)
        val totalSessions = sessions.size
        val totalSentences = sessions.map { session ->
            when (session.difficulty) {
                com.jpy.wordbook.model.Difficulty.EASY -> 100
                com.jpy.wordbook.model.Difficulty.MEDIUM -> 100
                com.jpy.wordbook.model.Difficulty.HARD -> 100
            }
        }.sum()

        // Group sessions by difficulty
        val easyCount = sessions.count { it.difficulty == com.jpy.wordbook.model.Difficulty.EASY }
        val mediumCount = sessions.count { it.difficulty == com.jpy.wordbook.model.Difficulty.MEDIUM }
        val hardCount = sessions.count { it.difficulty == com.jpy.wordbook.model.Difficulty.HARD }

        // Calculate streak (consecutive days with sessions)
        var streakDays = 0
        if (sessions.isNotEmpty()) {
            val today = java.time.LocalDate.now()
            var checkDate = today
            var i = 0

            while (i < sessions.size) {
                val sessionDate = java.time.LocalDateTime.ofInstant(
                    sessions[i].createdAt,
                    java.time.ZoneId.systemDefault()
                ).toLocalDate()

                if (sessionDate == checkDate) {
                    streakDays++
                    // Move to previous day
                    checkDate = checkDate.minusDays(1)
                    // Skip other sessions from the same day
                    while (i < sessions.size &&
                           java.time.LocalDateTime.ofInstant(
                               sessions[i].createdAt,
                               java.time.ZoneId.systemDefault()
                           ).toLocalDate() == sessionDate) {
                        i++
                    }
                } else if (sessionDate < checkDate) {
                    // Gap in streak
                    break
                } else {
                    i++
                }
            }
        }

        // Get topics distribution
        val topicsCount = sessions.mapNotNull { it.topic }.groupingBy { it }.eachCount()
        val topTopics = topicsCount.entries.sortedByDescending { it.value }.take(5)

        // Get word mastery progress
        val progressStats = progressService.getStats()
        val totalWords = vocabularyService.getWordCount()

        // Get XP and level info
        val levelInfo = xpService.getLevelInfo()

        return mapOf(
            "title" to "Progress Statistics - Tanoshii",
            "totalSessions" to totalSessions,
            "totalSentences" to totalSentences,
            "easyCount" to easyCount,
            "mediumCount" to mediumCount,
            "hardCount" to hardCount,
            "streakDays" to streakDays,
            "topTopics" to topTopics,
            "recentSessions" to sessions.take(10),
            // Word mastery stats
            "totalWords" to totalWords,
            "seenCount" to progressStats.masteryStats.seenCount,
            "learningCount" to progressStats.masteryStats.learningCount,
            "masteredCount" to progressStats.masteryStats.masteredCount,
            "totalEncountered" to progressStats.masteryStats.totalEncountered,
            "masteredPercentage" to progressStats.masteredPercentage,
            // XP and level stats
            "currentLevel" to levelInfo.currentLevel,
            "totalXp" to levelInfo.totalXp,
            "xpToNextLevel" to levelInfo.xpToNextLevel,
            "levelProgress" to levelInfo.progressPercentage,
            "nextLevelTotalXp" to levelInfo.nextLevelTotalXp
        )
    }
}
