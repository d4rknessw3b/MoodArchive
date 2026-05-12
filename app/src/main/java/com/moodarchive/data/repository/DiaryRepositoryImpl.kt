package com.moodarchive.data.repository

import com.moodarchive.data.local.DiaryDao
import com.moodarchive.data.local.EntityMapper.toDomain
import com.moodarchive.data.local.EntityMapper.toEntity
import com.moodarchive.data.remote.FirebaseDataSource
import com.moodarchive.domain.model.DiaryEntry
import com.moodarchive.domain.model.Emotion
import com.moodarchive.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация DiaryRepository: работает с Room локально,
 * синхронизируется с Firebase при наличии сети.
 */
@Singleton
class DiaryRepositoryImpl @Inject constructor(
    private val dao: DiaryDao,
    private val firebaseDataSource: FirebaseDataSource
) : DiaryRepository {

    override fun getAllEntries(): Flow<List<DiaryEntry>> {
        return dao.getAllEntries().map { entities ->
            entities.map { entity ->
                val attachments = dao.getAttachmentsByEntryId(entity.id).map { it.toDomain() }
                entity.toDomain(attachments)
            }
        }
    }

    override fun getEntriesByDate(startOfDay: Long, endOfDay: Long): Flow<List<DiaryEntry>> {
        return dao.getEntriesByDate(startOfDay, endOfDay).map { entities ->
            entities.map { entity ->
                val attachments = dao.getAttachmentsByEntryId(entity.id).map { it.toDomain() }
                entity.toDomain(attachments)
            }
        }
    }

    override fun getEntriesByEmotion(emotion: Emotion): Flow<List<DiaryEntry>> {
        return dao.getEntriesByEmotion(emotion.name).map { entities ->
            entities.map { entity ->
                val attachments = dao.getAttachmentsByEntryId(entity.id).map { it.toDomain() }
                entity.toDomain(attachments)
            }
        }
    }

    override fun searchEntries(query: String): Flow<List<DiaryEntry>> {
        return dao.searchEntries(query).map { entities ->
            entities.map { entity ->
                val attachments = dao.getAttachmentsByEntryId(entity.id).map { it.toDomain() }
                entity.toDomain(attachments)
            }
        }
    }

    override suspend fun getEntryById(id: String): DiaryEntry? {
        val entity = dao.getEntryById(id) ?: return null
        val attachments = dao.getAttachmentsByEntryId(id).map { it.toDomain() }
        return entity.toDomain(attachments)
    }

    override suspend fun insertEntry(entry: DiaryEntry): String {
        val id = if (entry.id.isBlank()) UUID.randomUUID().toString() else entry.id
        val entryWithId = entry.copy(id = id)
        dao.insertEntry(entryWithId.toEntity())
        // Сохраняем вложения
        entryWithId.attachments.forEach { attachment ->
            dao.insertAttachment(attachment.copy(entryId = id).toEntity())
        }
        // Попытка синхронизации с Firebase
        firebaseDataSource.uploadEntry(entryWithId)
        return id
    }

    override suspend fun updateEntry(entry: DiaryEntry) {
        val updated = entry.copy(updatedAt = System.currentTimeMillis())
        dao.updateEntry(updated.toEntity())
        // Обновляем вложения
        dao.deleteAttachmentsByEntryId(entry.id)
        entry.attachments.forEach { attachment ->
            dao.insertAttachment(attachment.copy(entryId = entry.id).toEntity())
        }
        firebaseDataSource.uploadEntry(updated)
    }

    override suspend fun deleteEntry(id: String) {
        dao.deleteEntry(id) // Вложения удалятся каскадно
        firebaseDataSource.deleteEntry(id)
    }

    override fun getEmotionStats(startDate: Long, endDate: Long): Flow<Map<Emotion, Int>> {
        return dao.getEmotionCounts(startDate, endDate).map { counts ->
            counts.associate { Emotion.fromName(it.emotionName) to it.count }
        }
    }

    override fun getDatesWithEntries(): Flow<List<Long>> {
        return dao.getAllDates()
    }

    override suspend fun syncPendingEntries() {
        // 1. Fetch from Firebase
        val fetchResult = firebaseDataSource.fetchAllEntries()
        if (fetchResult.isSuccess) {
            val remoteEntries = fetchResult.getOrNull() ?: emptyList()
            for (entry in remoteEntries) {
                val localEntity = dao.getEntryById(entry.id)
                if (localEntity == null) {
                    dao.insertEntry(entry.toEntity())
                } else if (entry.updatedAt > localEntity.updatedAt) {
                    dao.updateEntry(entry.toEntity())
                }
            }
        }

        // 2. Upload unsynced local entries
        val unsynced = dao.getUnsyncedEntries()
        for (entry in unsynced) {
            val attachments = dao.getAttachmentsByEntryId(entry.id).map { it.toDomain() }
            val result = firebaseDataSource.uploadEntry(entry.toDomain(attachments))
            if (result.isSuccess) {
                dao.markAsSynced(entry.id)
            }
        }
    }
    override suspend fun getAllEntriesOnce(): List<DiaryEntry> {
        return dao.getAllEntriesOnce().map { entity ->
            val attachments = dao.getAttachmentsByEntryId(entity.id).map { it.toDomain() }
            entity.toDomain(attachments)
        }
    }
}
