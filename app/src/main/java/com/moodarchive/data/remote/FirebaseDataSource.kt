package com.moodarchive.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.moodarchive.domain.model.DiaryEntry
import com.moodarchive.domain.model.Emotion
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Источник данных Firebase: Firestore для записей, Auth для идентификации пользователя.
 * Медиафайлы хранятся только локально (Firebase Storage не используется).
 *
 * Структура Firestore:
 *   users/{uid}/entries/{entryId} → поля записи
 */
@Singleton
class FirebaseDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    /** Ссылка на коллекцию записей текущего пользователя */
    private fun entriesCollection(uid: String) =
        firestore.collection("users").document(uid).collection("entries")

    /**
     * Загрузить (создать или обновить) запись в Firestore.
     */
    suspend fun uploadEntry(entry: DiaryEntry): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Пользователь не авторизован"))
        return try {
            val data = mapOf(
                "text"         to entry.text,
                "emotionName"  to entry.emotion.name,
                "createdAt"    to entry.createdAt,
                "updatedAt"    to entry.updatedAt,
                "latitude"     to entry.latitude,
                "longitude"    to entry.longitude,
                "locationName" to entry.locationName
            )
            entriesCollection(uid)
                .document(entry.id)
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Удалить запись из Firestore.
     */
    suspend fun deleteEntry(id: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Пользователь не авторизован"))
        return try {
            entriesCollection(uid).document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Получить все записи пользователя из Firestore.
     * Используется при первичной синхронизации / смене устройства.
     */
    suspend fun fetchAllEntries(): Result<List<DiaryEntry>> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Пользователь не авторизован"))
        return try {
            val snapshot = entriesCollection(uid)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val entries = snapshot.documents.mapNotNull { doc ->
                try {
                    DiaryEntry(
                        id           = doc.id,
                        text         = doc.getString("text") ?: "",
                        emotion      = Emotion.fromName(doc.getString("emotionName") ?: "NEUTRAL"),
                        createdAt    = doc.getLong("createdAt") ?: 0L,
                        updatedAt    = doc.getLong("updatedAt") ?: 0L,
                        latitude     = doc.getDouble("latitude"),
                        longitude    = doc.getDouble("longitude"),
                        locationName = doc.getString("locationName"),
                        isSynced     = true
                    )
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
