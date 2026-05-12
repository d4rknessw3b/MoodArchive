package com.moodarchive.data.local

import com.moodarchive.domain.model.Attachment
import com.moodarchive.domain.model.AttachmentType
import com.moodarchive.domain.model.DiaryEntry
import com.moodarchive.domain.model.Emotion

/**
 * Маппер между Room-сущностями и доменными моделями.
 */
object EntityMapper {

    fun DiaryEntryEntity.toDomain(attachments: List<Attachment> = emptyList()): DiaryEntry {
        return DiaryEntry(
            id = id,
            text = text,
            emotion = Emotion.fromName(emotionName),
            createdAt = createdAt,
            updatedAt = updatedAt,
            attachments = attachments,
            latitude = latitude,
            longitude = longitude,
            locationName = locationName,
            isSynced = isSynced
        )
    }

    fun DiaryEntry.toEntity(): DiaryEntryEntity {
        return DiaryEntryEntity(
            id = id,
            text = text,
            emotionName = emotion.name,
            createdAt = createdAt,
            updatedAt = updatedAt,
            latitude = latitude,
            longitude = longitude,
            locationName = locationName,
            isSynced = isSynced
        )
    }

    fun AttachmentEntity.toDomain(): Attachment {
        return Attachment(
            id = id,
            entryId = entryId,
            type = try {
                AttachmentType.valueOf(typeName)
            } catch (_: Exception) {
                AttachmentType.FILE
            },
            localUri = localUri,
            remoteUrl = remoteUrl,
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = sizeBytes
        )
    }

    fun Attachment.toEntity(): AttachmentEntity {
        return AttachmentEntity(
            id = id,
            entryId = entryId,
            typeName = type.name,
            localUri = localUri,
            remoteUrl = remoteUrl,
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = sizeBytes
        )
    }
}
