package com.jpy.wordbook.model

import java.time.Instant

enum class Difficulty {
    EASY, MEDIUM, HARD
}

data class Session(
    val id: String,
    val difficulty: Difficulty,
    val createdAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val currentTaskIndex: Int = 0
)
