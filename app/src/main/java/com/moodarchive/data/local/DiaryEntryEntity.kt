package com.moodarchive.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room-сущность для хранения записи дневника в локальной БД.
 */
@Entity(tableName = "diary_entries")
data class DiaryEntryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val emotionName: String = "NEUTRAL",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val isSynced: Boolean = false
)
