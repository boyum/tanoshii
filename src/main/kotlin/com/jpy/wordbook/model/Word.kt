package com.jpy.wordbook.model

data class Word(
    val id: Long? = null,
    val japanese: String,
    val romaji: String,
    val english: String,
    val category: String,
    val categoryName: String
)
