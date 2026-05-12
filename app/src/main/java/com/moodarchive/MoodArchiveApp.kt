package com.moodarchive

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.moodarchive.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Точка входа приложения MoodArchive.
 * Аннотация @HiltAndroidApp инициализирует Hilt DI.
 * Implements Configuration.Provider для интеграции WorkManager + Hilt.
 */
@HiltAndroidApp
class MoodArchiveApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Канал уведомлений необходимо создать до первого показа уведомления.
        // На Android 8+ (API 26) без канала уведомления не отображаются.
        // Повторный вызов безопасен — система игнорирует уже существующий канал.
        NotificationHelper.createNotificationChannel(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
