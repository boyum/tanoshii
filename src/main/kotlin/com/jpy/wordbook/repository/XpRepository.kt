package com.jpy.wordbook.repository

import jakarta.inject.Singleton
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import javax.sql.DataSource

data class UserXp(
    val id: Long = 1,
    val totalXp: Int,
    val currentLevel: Int,
    val lastStreakBonusDate: LocalDate?,
    val updatedAt: Instant
)

data class XpActivity(
    val id: Long? = null,
    val activityType: String,
    val xpGained: Int,
    val description: String?,
    val createdAt: Instant = Instant.now()
)

enum class ActivityType(val type: String) {
    TASK("task"),
    SESSION("session"),
    STREAK("streak"),
    NEW_WORD("new_word")
}

@Singleton
class XpRepository(private val dataSource: DataSource) {

    /**
     * Get the current user XP status
     */
    fun getUserXp(): UserXp {
        dataSource.connection.use { conn ->
            val sql = "SELECT * FROM user_xp WHERE id = 1"
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) {
                        UserXp(
                            id = rs.getLong("id"),
                            totalXp = rs.getInt("total_xp"),
                            currentLevel = rs.getInt("current_level"),
                            lastStreakBonusDate = rs.getDate("last_streak_bonus_date")?.toLocalDate(),
                            updatedAt = rs.getTimestamp("updated_at").toInstant()
                        )
                    } else {
                        // Fallback if no row exists
                        UserXp(1, 0, 1, null, Instant.now())
                    }
                }
            }
        }
    }

    /**
     * Add XP and update level
     */
    fun addXp(xpGained: Int, activityType: ActivityType, description: String? = null): UserXp {
        dataSource.connection.use { conn ->
            // Start transaction
            conn.autoCommit = false

            try {
                // Get current XP
                val currentXp = getUserXp()
                val newTotalXp = currentXp.totalXp + xpGained
                val newLevel = calculateLevel(newTotalXp)

                // Update user XP
                val updateSql = """
                    UPDATE user_xp
                    SET total_xp = ?, current_level = ?, updated_at = ?
                    WHERE id = 1
                """.trimIndent()
                conn.prepareStatement(updateSql).use { stmt ->
                    stmt.setInt(1, newTotalXp)
                    stmt.setInt(2, newLevel)
                    stmt.setTimestamp(3, Timestamp.from(Instant.now()))
                    stmt.executeUpdate()
                }

                // Log activity
                val logSql = """
                    INSERT INTO xp_activities (activity_type, xp_gained, description)
                    VALUES (?, ?, ?)
                """.trimIndent()
                conn.prepareStatement(logSql).use { stmt ->
                    stmt.setString(1, activityType.type)
                    stmt.setInt(2, xpGained)
                    stmt.setString(3, description)
                    stmt.executeUpdate()
                }

                conn.commit()

                // Return updated XP
                return getUserXp()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = true
            }
        }
    }

    /**
     * Record streak bonus for today
     */
    fun recordStreakBonus(xpGained: Int): UserXp {
        dataSource.connection.use { conn ->
            conn.autoCommit = false

            try {
                val today = LocalDate.now()

                // Update last streak bonus date
                val updateSql = """
                    UPDATE user_xp
                    SET last_streak_bonus_date = ?
                    WHERE id = 1
                """.trimIndent()
                conn.prepareStatement(updateSql).use { stmt ->
                    stmt.setDate(1, java.sql.Date.valueOf(today))
                    stmt.executeUpdate()
                }

                conn.commit()

                // Then add XP
                return addXp(xpGained, ActivityType.STREAK, "Daily streak bonus")
            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = true
            }
        }
    }

    /**
     * Check if streak bonus has been claimed today
     */
    fun hasClaimedStreakBonusToday(): Boolean {
        val userXp = getUserXp()
        return userXp.lastStreakBonusDate == LocalDate.now()
    }

    /**
     * Get recent XP activities
     */
    fun getRecentActivities(limit: Int = 10): List<XpActivity> {
        dataSource.connection.use { conn ->
            val sql = """
                SELECT * FROM xp_activities
                ORDER BY created_at DESC
                LIMIT ?
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, limit)
                stmt.executeQuery().use { rs ->
                    val activities = mutableListOf<XpActivity>()
                    while (rs.next()) {
                        activities.add(
                            XpActivity(
                                id = rs.getLong("id"),
                                activityType = rs.getString("activity_type"),
                                xpGained = rs.getInt("xp_gained"),
                                description = rs.getString("description"),
                                createdAt = rs.getTimestamp("created_at").toInstant()
                            )
                        )
                    }
                    return activities
                }
            }
        }
    }

    /**
     * Calculate level from total XP using formula: level^2 * 50
     */
    fun calculateLevel(totalXp: Int): Int {
        var level = 1
        while (getXpRequiredForLevel(level + 1) <= totalXp) {
            level++
        }
        return level
    }

    /**
     * Get total XP required to reach a specific level
     */
    fun getXpRequiredForLevel(level: Int): Int {
        return level * level * 50
    }

    /**
     * Get XP required for next level from current XP
     */
    fun getXpToNextLevel(currentXp: Int, currentLevel: Int): Int {
        val nextLevelXp = getXpRequiredForLevel(currentLevel + 1)
        return nextLevelXp - currentXp
    }

    /**
     * Get progress percentage to next level
     */
    fun getProgressToNextLevel(currentXp: Int, currentLevel: Int): Int {
        val currentLevelXp = getXpRequiredForLevel(currentLevel)
        val nextLevelXp = getXpRequiredForLevel(currentLevel + 1)
        val xpInCurrentLevel = currentXp - currentLevelXp
        val xpNeededForLevel = nextLevelXp - currentLevelXp

        return if (xpNeededForLevel > 0) {
            ((xpInCurrentLevel * 100) / xpNeededForLevel).coerceIn(0, 100)
        } else {
            100
        }
    }
}
