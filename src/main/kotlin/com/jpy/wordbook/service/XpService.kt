package com.jpy.wordbook.service

import com.jpy.wordbook.model.Difficulty
import com.jpy.wordbook.repository.ActivityType
import com.jpy.wordbook.repository.UserXp
import com.jpy.wordbook.repository.XpRepository
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

data class LevelInfo(
    val currentLevel: Int,
    val totalXp: Int,
    val xpToNextLevel: Int,
    val progressPercentage: Int,
    val nextLevelTotalXp: Int
)

data class XpGainResult(
    val xpGained: Int,
    val newTotalXp: Int,
    val oldLevel: Int,
    val newLevel: Int,
    val leveledUp: Boolean
)

@Singleton
class XpService(
    private val xpRepository: XpRepository
) {
    private val logger = LoggerFactory.getLogger(XpService::class.java)

    companion object {
        // XP values for different activities
        const val XP_TASK_EASY = 10
        const val XP_TASK_MEDIUM = 15
        const val XP_TASK_HARD = 20
        const val XP_SESSION_COMPLETE = 50
        const val XP_STREAK_BONUS = 50
        const val XP_NEW_WORD = 5
    }

    /**
     * Award XP for completing a task
     */
    fun awardTaskXp(difficulty: Difficulty): XpGainResult {
        val xpAmount = when (difficulty) {
            Difficulty.EASY -> XP_TASK_EASY
            Difficulty.MEDIUM -> XP_TASK_MEDIUM
            Difficulty.HARD -> XP_TASK_HARD
        }

        val oldXp = xpRepository.getUserXp()
        val newXp = xpRepository.addXp(
            xpAmount,
            ActivityType.TASK,
            "Completed ${difficulty.name.lowercase()} task"
        )

        logger.info("Awarded $xpAmount XP for ${difficulty.name} task (Total: ${newXp.totalXp})")

        return createXpGainResult(xpAmount, oldXp, newXp)
    }

    /**
     * Award XP for completing a full session
     */
    fun awardSessionXp(sessionId: String, difficulty: Difficulty): XpGainResult {
        val oldXp = xpRepository.getUserXp()
        val newXp = xpRepository.addXp(
            XP_SESSION_COMPLETE,
            ActivityType.SESSION,
            "Completed ${difficulty.name.lowercase()} session: $sessionId"
        )

        logger.info("Awarded $XP_SESSION_COMPLETE XP for session completion (Total: ${newXp.totalXp})")

        return createXpGainResult(XP_SESSION_COMPLETE, oldXp, newXp)
    }

    /**
     * Award XP for maintaining streak (once per day)
     */
    fun awardStreakBonus(): XpGainResult? {
        if (xpRepository.hasClaimedStreakBonusToday()) {
            logger.debug("Streak bonus already claimed today")
            return null
        }

        val oldXp = xpRepository.getUserXp()
        val newXp = xpRepository.recordStreakBonus(XP_STREAK_BONUS)

        logger.info("Awarded $XP_STREAK_BONUS XP for daily streak bonus (Total: ${newXp.totalXp})")

        return createXpGainResult(XP_STREAK_BONUS, oldXp, newXp)
    }

    /**
     * Award XP for discovering a new word
     */
    fun awardNewWordXp(wordId: Long, wordText: String): XpGainResult {
        val oldXp = xpRepository.getUserXp()
        val newXp = xpRepository.addXp(
            XP_NEW_WORD,
            ActivityType.NEW_WORD,
            "Discovered new word: $wordText"
        )

        logger.debug("Awarded $XP_NEW_WORD XP for new word: $wordText")

        return createXpGainResult(XP_NEW_WORD, oldXp, newXp)
    }

    /**
     * Get current level information
     */
    fun getLevelInfo(): LevelInfo {
        val userXp = xpRepository.getUserXp()
        val xpToNext = xpRepository.getXpToNextLevel(userXp.totalXp, userXp.currentLevel)
        val progress = xpRepository.getProgressToNextLevel(userXp.totalXp, userXp.currentLevel)
        val nextLevelXp = xpRepository.getXpRequiredForLevel(userXp.currentLevel + 1)

        return LevelInfo(
            currentLevel = userXp.currentLevel,
            totalXp = userXp.totalXp,
            xpToNextLevel = xpToNext,
            progressPercentage = progress,
            nextLevelTotalXp = nextLevelXp
        )
    }

    /**
     * Get user XP data
     */
    fun getUserXp(): UserXp {
        return xpRepository.getUserXp()
    }

    /**
     * Helper to create XP gain result
     */
    private fun createXpGainResult(xpGained: Int, oldXp: UserXp, newXp: UserXp): XpGainResult {
        val leveledUp = newXp.currentLevel > oldXp.currentLevel

        if (leveledUp) {
            logger.info("🎉 LEVEL UP! ${oldXp.currentLevel} → ${newXp.currentLevel}")
        }

        return XpGainResult(
            xpGained = xpGained,
            newTotalXp = newXp.totalXp,
            oldLevel = oldXp.currentLevel,
            newLevel = newXp.currentLevel,
            leveledUp = leveledUp
        )
    }
}
