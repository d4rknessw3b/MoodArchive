package com.moodarchive.domain.model

/**
 * Вложение к записи дневника (фото, видео, аудио, файл).
 */
data class Attachment(
    val id: String = "",
    val entryId: String = "",
    val type: AttachmentType,
    val localUri: String = "",
    val remoteUrl: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val sizeBytes: Long = 0L
)
