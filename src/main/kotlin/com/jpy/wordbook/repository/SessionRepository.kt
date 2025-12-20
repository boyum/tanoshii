package com.jpy.wordbook.repository

import com.jpy.wordbook.model.Difficulty
import com.jpy.wordbook.model.Session
import jakarta.inject.Singleton
import javax.sql.DataSource
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

@Singleton
class SessionRepository(private val dataSource: DataSource) {

    fun save(session: Session): Session {
        dataSource.connection.use { conn ->
            val sql = """
                INSERT INTO sessions (id, difficulty, created_at, completed_at, current_task_index)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, session.id)
                stmt.setString(2, session.difficulty.name.lowercase())
                stmt.setTimestamp(3, Timestamp.from(session.createdAt))
                stmt.setTimestamp(4, session.completedAt?.let { Timestamp.from(it) })
                stmt.setInt(5, session.currentTaskIndex)
                stmt.executeUpdate()
            }
        }
        return session
    }

    fun findById(id: String): Session? {
        dataSource.connection.use { conn ->
            val sql = "SELECT * FROM sessions WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, id)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) mapRow(rs) else null
                }
            }
        }
    }

    fun updateCurrentIndex(id: String, index: Int) {
        dataSource.connection.use { conn ->
            val sql = "UPDATE sessions SET current_task_index = ? WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, index)
                stmt.setString(2, id)
                stmt.executeUpdate()
            }
        }
    }

    fun markCompleted(id: String) {
        dataSource.connection.use { conn ->
            val sql = "UPDATE sessions SET completed_at = ? WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setTimestamp(1, Timestamp.from(Instant.now()))
                stmt.setString(2, id)
                stmt.executeUpdate()
            }
        }
    }

    private fun mapRow(rs: ResultSet): Session {
        return Session(
            id = rs.getString("id"),
            difficulty = Difficulty.valueOf(rs.getString("difficulty").uppercase()),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            completedAt = rs.getTimestamp("completed_at")?.toInstant(),
            currentTaskIndex = rs.getInt("current_task_index")
        )
    }
}
