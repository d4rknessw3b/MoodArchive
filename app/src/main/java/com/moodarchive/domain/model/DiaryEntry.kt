package com.moodarchive.domain.model

/**
 * Основная доменная модель — запись дневника.
 */
data class DiaryEntry(
    val id: String = "",
    val text: String = "",
    val emotion: Emotion = Emotion.NEUTRAL,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val attachments: List<Attachment> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val isSynced: Boolean = false
)
