package com.moodarchive.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с записями дневника и вложениями.
 */
@Dao
interface DiaryDao {

    // ─── Записи ───────────────────────────────────────────

    @Query("SELECT * FROM diary_entries ORDER BY createdAt DESC")
    fun getAllEntries(): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entries WHERE createdAt BETWEEN :start AND :end ORDER BY createdAt DESC")
    fun getEntriesByDate(start: Long, end: Long): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entries WHERE emotionName = :emotionName ORDER BY createdAt DESC")
    fun getEntriesByEmotion(emotionName: String): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entries WHERE text LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchEntries(query: String): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getEntryById(id: String): DiaryEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DiaryEntryEntity)

    @Update
    suspend fun updateEntry(entry: DiaryEntryEntity)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun deleteEntry(id: String)

    @Query("SELECT emotionName, COUNT(*) as count FROM diary_entries WHERE createdAt BETWEEN :start AND :end GROUP BY emotionName")
    fun getEmotionCounts(start: Long, end: Long): Flow<List<EmotionCount>>

    @Query("SELECT DISTINCT createdAt FROM diary_entries ORDER BY createdAt DESC")
    fun getAllDates(): Flow<List<Long>>

    @Query("SELECT * FROM diary_entries WHERE isSynced = 0")
    suspend fun getUnsyncedEntries(): List<DiaryEntryEntity>

    @Query("SELECT * FROM diary_entries ORDER BY createdAt DESC")
    suspend fun getAllEntriesOnce(): List<DiaryEntryEntity>

    @Query("UPDATE diary_entries SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    // ─── Вложения ─────────────────────────────────────────

    @Query("SELECT * FROM attachments WHERE entryId = :entryId")
    suspend fun getAttachmentsByEntryId(entryId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE entryId = :entryId")
    fun getAttachmentsByEntryIdFlow(entryId: String): Flow<List<AttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteAttachment(id: String)

    @Query("DELETE FROM attachments WHERE entryId = :entryId")
    suspend fun deleteAttachmentsByEntryId(entryId: String)
}

/**
 * Вспомогательный класс для результата агрегации эмоций.
 */
data class EmotionCount(
    val emotionName: String,
    val count: Int
)
