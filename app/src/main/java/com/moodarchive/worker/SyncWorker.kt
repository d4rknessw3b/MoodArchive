package com.moodarchive.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moodarchive.domain.repository.DiaryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Фоновый Worker для синхронизации локальных записей с Firebase.
 * Запускается WorkManager по расписанию или при подключении к сети.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val diaryRepository: DiaryRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            diaryRepository.syncPendingEntries()
            Result.success()
        } catch (e: Exception) {
            // Retry up to 3 times on failure
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "mood_archive_sync"
    }
}
