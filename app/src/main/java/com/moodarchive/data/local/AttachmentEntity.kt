package com.moodarchive.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room-сущность для хранения вложения (фото/видео/аудио/файл).
 */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entryId")]
)
data class AttachmentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val entryId: String = "",
    val typeName: String = "FILE",
    val localUri: String = "",
    val remoteUrl: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val sizeBytes: Long = 0L
)
