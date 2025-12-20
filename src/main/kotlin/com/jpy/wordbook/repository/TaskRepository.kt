package com.jpy.wordbook.repository

import com.jpy.wordbook.model.Task
import com.jpy.wordbook.model.TaskType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.inject.Singleton
import javax.sql.DataSource
import java.sql.ResultSet
import java.sql.Statement

@Singleton
class TaskRepository(private val dataSource: DataSource) {

    private val objectMapper = jacksonObjectMapper()

    fun save(task: Task): Task {
        dataSource.connection.use { conn ->
            val sql = """
                INSERT INTO tasks (session_id, task_index, task_type, japanese_text, english_translation, word_ids, audio_hash, furigana_text, romaji_text)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, task.sessionId)
                stmt.setInt(2, task.taskIndex)
                stmt.setString(3, task.taskType.name.lowercase())
                stmt.setString(4, task.japaneseText)
                stmt.setString(5, task.englishTranslation)
                stmt.setString(6, objectMapper.writeValueAsString(task.wordIds))
                stmt.setString(7, task.audioHash)
                stmt.setString(8, task.furiganaText)
                stmt.setString(9, task.romajiText)
                stmt.executeUpdate()
            }

            // SQLite: get last inserted row ID
            conn.prepareStatement("SELECT last_insert_rowid()").use { stmt ->
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        return task.copy(id = rs.getLong(1))
                    }
                }
            }
        }
        throw RuntimeException("Failed to save task")
    }

    fun saveAll(tasks: List<Task>): List<Task> {
        return tasks.map { save(it) }
    }

    fun findBySessionId(sessionId: String): List<Task> {
        dataSource.connection.use { conn ->
            val sql = "SELECT * FROM tasks WHERE session_id = ? ORDER BY task_index"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, sessionId)
                stmt.executeQuery().use { rs ->
                    return generateSequence { if (rs.next()) mapRow(rs) else null }.toList()
                }
            }
        }
    }

    fun findBySessionIdAndIndex(sessionId: String, taskIndex: Int): Task? {
        dataSource.connection.use { conn ->
            val sql = "SELECT * FROM tasks WHERE session_id = ? AND task_index = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, sessionId)
                stmt.setInt(2, taskIndex)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) mapRow(rs) else null
                }
            }
        }
    }

    fun updateAudioHash(taskId: Long, audioHash: String) {
        dataSource.connection.use { conn ->
            val sql = "UPDATE tasks SET audio_hash = ? WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, audioHash)
                stmt.setLong(2, taskId)
                stmt.executeUpdate()
            }
        }
    }

    fun countBySessionId(sessionId: String): Int {
        dataSource.connection.use { conn ->
            val sql = "SELECT COUNT(*) FROM tasks WHERE session_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, sessionId)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }

    private fun mapRow(rs: ResultSet): Task {
        return Task(
            id = rs.getLong("id"),
            sessionId = rs.getString("session_id"),
            taskIndex = rs.getInt("task_index"),
            taskType = TaskType.valueOf(rs.getString("task_type").uppercase()),
            japaneseText = rs.getString("japanese_text"),
            englishTranslation = rs.getString("english_translation"),
            wordIds = objectMapper.readValue(rs.getString("word_ids")),
            audioHash = rs.getString("audio_hash"),
            furiganaText = rs.getString("furigana_text"),
            romajiText = rs.getString("romaji_text")
        )
    }
}
