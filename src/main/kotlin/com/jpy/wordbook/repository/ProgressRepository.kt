package com.jpy.wordbook.repository

import jakarta.inject.Singleton
import javax.sql.DataSource
import java.sql.Timestamp
import java.time.Instant

data class WordProgress(
    val id: Long? = null,
    val wordId: Long,
    val timesSeen: Int = 0,
    val lastSeenAt: Instant? = null
)

@Singleton
class ProgressRepository(private val dataSource: DataSource) {

    fun recordWordSeen(wordId: Long) {
        dataSource.connection.use { conn ->
            val sql = """
                INSERT INTO word_progress (word_id, times_seen, last_seen_at)
                VALUES (?, 1, ?)
                ON CONFLICT(word_id) DO UPDATE SET
                    times_seen = times_seen + 1,
                    last_seen_at = excluded.last_seen_at
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, wordId)
                stmt.setTimestamp(2, Timestamp.from(Instant.now()))
                stmt.executeUpdate()
            }
        }
    }

    fun recordWordsSeen(wordIds: List<Long>) {
        wordIds.forEach { recordWordSeen(it) }
    }

    fun getProgress(wordId: Long): WordProgress? {
        dataSource.connection.use { conn ->
            val sql = "SELECT * FROM word_progress WHERE word_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, wordId)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) {
                        WordProgress(
                            id = rs.getLong("id"),
                            wordId = rs.getLong("word_id"),
                            timesSeen = rs.getInt("times_seen"),
                            lastSeenAt = rs.getTimestamp("last_seen_at")?.toInstant()
                        )
                    } else null
                }
            }
        }
    }

    fun getTotalWordsSeen(): Long {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT COUNT(*) FROM word_progress WHERE times_seen > 0").use { stmt ->
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.getLong(1) else 0
                }
            }
        }
    }

    fun getTotalPracticeCount(): Long {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT SUM(times_seen) FROM word_progress").use { stmt ->
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.getLong(1) else 0
                }
            }
        }
    }

    fun getCompletedSessionsCount(): Long {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT COUNT(*) FROM sessions WHERE completed_at IS NOT NULL").use { stmt ->
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.getLong(1) else 0
                }
            }
        }
    }

    /**
     * Get word mastery statistics by level:
     * - Seen (1-2 encounters)
     * - Learning (3-5 encounters)
     * - Mastered (6+ encounters)
     */
    fun getWordMasteryStats(): MasteryStats {
        dataSource.connection.use { conn ->
            val sql = """
                SELECT
                    COUNT(CASE WHEN times_seen >= 1 AND times_seen <= 2 THEN 1 END) as seen_count,
                    COUNT(CASE WHEN times_seen >= 3 AND times_seen <= 5 THEN 1 END) as learning_count,
                    COUNT(CASE WHEN times_seen >= 6 THEN 1 END) as mastered_count
                FROM word_progress
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) {
                        MasteryStats(
                            seenCount = rs.getLong("seen_count"),
                            learningCount = rs.getLong("learning_count"),
                            masteredCount = rs.getLong("mastered_count")
                        )
                    } else {
                        MasteryStats(0, 0, 0)
                    }
                }
            }
        }
    }

    /**
     * Get the mastery level for a specific word
     */
    fun getWordMasteryLevel(wordId: Long): MasteryLevel {
        val progress = getProgress(wordId)
        return when {
            progress == null || progress.timesSeen == 0 -> MasteryLevel.UNSEEN
            progress.timesSeen <= 2 -> MasteryLevel.SEEN
            progress.timesSeen <= 5 -> MasteryLevel.LEARNING
            else -> MasteryLevel.MASTERED
        }
    }
}

enum class MasteryLevel(val displayName: String, val emoji: String) {
    UNSEEN("Unseen", "⚪"),
    SEEN("Seen", "👀"),
    LEARNING("Learning", "📖"),
    MASTERED("Mastered", "✅")
}

data class MasteryStats(
    val seenCount: Long,
    val learningCount: Long,
    val masteredCount: Long
) {
    val totalEncountered: Long
        get() = seenCount + learningCount + masteredCount
}
