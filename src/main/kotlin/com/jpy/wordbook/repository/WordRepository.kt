package com.jpy.wordbook.repository

import com.jpy.wordbook.model.Word
import jakarta.inject.Singleton
import javax.sql.DataSource
import java.sql.ResultSet

@Singleton
class WordRepository(private val dataSource: DataSource) {

    fun save(word: Word): Word {
        dataSource.connection.use { conn ->
            val sql = """
                INSERT INTO words (japanese, romaji, english, category, category_name)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, word.japanese)
                stmt.setString(2, word.romaji)
                stmt.setString(3, word.english)
                stmt.setString(4, word.category)
                stmt.setString(5, word.categoryName)
                stmt.executeUpdate()
            }

            // SQLite: get last inserted row ID
            conn.prepareStatement("SELECT last_insert_rowid()").use { stmt ->
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        return word.copy(id = rs.getLong(1))
                    }
                }
            }
        }
        throw RuntimeException("Failed to save word")
    }

    fun saveAll(words: List<Word>): List<Word> {
        dataSource.connection.use { conn ->
            val sql = """
                INSERT INTO words (japanese, romaji, english, category, category_name)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                for (word in words) {
                    stmt.setString(1, word.japanese)
                    stmt.setString(2, word.romaji)
                    stmt.setString(3, word.english)
                    stmt.setString(4, word.category)
                    stmt.setString(5, word.categoryName)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }
        return words
    }

    fun findAll(): List<Word> {
        dataSource.connection.use { conn ->
            val sql = "SELECT * FROM words ORDER BY category, id"
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    return generateSequence { if (rs.next()) mapRow(rs) else null }.toList()
                }
            }
        }
    }

    fun findByIds(ids: List<Long>): List<Word> {
        if (ids.isEmpty()) return emptyList()

        dataSource.connection.use { conn ->
            val placeholders = ids.joinToString(",") { "?" }
            val sql = "SELECT * FROM words WHERE id IN ($placeholders)"
            conn.prepareStatement(sql).use { stmt ->
                ids.forEachIndexed { index, id ->
                    stmt.setLong(index + 1, id)
                }
                stmt.executeQuery().use { rs ->
                    return generateSequence { if (rs.next()) mapRow(rs) else null }.toList()
                }
            }
        }
    }

    fun findRandomWords(count: Int): List<Word> {
        dataSource.connection.use { conn ->
            val sql = "SELECT * FROM words ORDER BY RANDOM() LIMIT ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, count)
                stmt.executeQuery().use { rs ->
                    return generateSequence { if (rs.next()) mapRow(rs) else null }.toList()
                }
            }
        }
    }

    fun count(): Long {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT COUNT(*) FROM words").use { stmt ->
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.getLong(1) else 0
                }
            }
        }
    }

    fun deleteAll() {
        dataSource.connection.use { conn ->
            conn.prepareStatement("DELETE FROM words").use { stmt ->
                stmt.executeUpdate()
            }
        }
    }

    fun getCategories(): List<String> {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT DISTINCT category_name FROM words ORDER BY category").use { stmt ->
                stmt.executeQuery().use { rs ->
                    return generateSequence { if (rs.next()) rs.getString(1) else null }.toList()
                }
            }
        }
    }

    private fun mapRow(rs: ResultSet): Word {
        return Word(
            id = rs.getLong("id"),
            japanese = rs.getString("japanese"),
            romaji = rs.getString("romaji"),
            english = rs.getString("english"),
            category = rs.getString("category"),
            categoryName = rs.getString("category_name")
        )
    }
}
