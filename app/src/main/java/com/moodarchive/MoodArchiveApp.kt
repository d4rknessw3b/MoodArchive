package com.moodarchive

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
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

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
