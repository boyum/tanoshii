package com.jpy.wordbook.model

enum class TaskType {
    SENTENCE, STORY
}

data class Task(
    val id: Long? = null,
    val sessionId: String,
    val taskIndex: Int,
    val taskType: TaskType,
    val japaneseText: String,
    val englishTranslation: String,
    val wordIds: List<Long>,
    val audioHash: String? = null,
    val furiganaText: String? = null,
    val romajiText: String? = null,
    val wordTranslations: Map<String, String>? = null  // Japanese word -> English meaning
)
