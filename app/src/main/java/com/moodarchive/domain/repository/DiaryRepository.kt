package com.moodarchive.domain.repository

import com.moodarchive.domain.model.DiaryEntry
import com.moodarchive.domain.model.Emotion
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с записями дневника.
 * Реализация может использовать Room (локально) и Firebase (удалённо).
 */
interface DiaryRepository {

    /** Получить все записи, отсортированные по дате (новые сверху) */
    fun getAllEntries(): Flow<List<DiaryEntry>>

    /** Получить записи за конкретную дату (timestamp начала и конца дня) */
    fun getEntriesByDate(startOfDay: Long, endOfDay: Long): Flow<List<DiaryEntry>>

    /** Получить записи по эмоции */
    fun getEntriesByEmotion(emotion: Emotion): Flow<List<DiaryEntry>>

    /** Поиск записей по ключевому слову */
    fun searchEntries(query: String): Flow<List<DiaryEntry>>

    /** Получить конкретную запись по ID */
    suspend fun getEntryById(id: String): DiaryEntry?

    /** Добавить новую запись */
    suspend fun insertEntry(entry: DiaryEntry): String

    /** Обновить существующую запись */
    suspend fun updateEntry(entry: DiaryEntry)

    /** Удалить запись */
    suspend fun deleteEntry(id: String)

    /** Получить количество записей по каждой эмоции за период */
    fun getEmotionStats(startDate: Long, endDate: Long): Flow<Map<Emotion, Int>>

    /** Получить даты, за которые есть записи (для календаря) */
    fun getDatesWithEntries(): Flow<List<Long>>

    /** Получить все записи единоразово (для экспорта) */
    suspend fun getAllEntriesOnce(): List<DiaryEntry>

    /** Синхронизировать несинхронизированные записи с Firebase */
    suspend fun syncPendingEntries()
}
