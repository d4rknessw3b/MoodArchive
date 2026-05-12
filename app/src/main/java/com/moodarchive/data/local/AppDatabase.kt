package com.moodarchive.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Основная Room-база данных MoodArchive.
 */
@Database(
    entities = [DiaryEntryEntity::class, AttachmentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao

    companion object {
        const val DATABASE_NAME = "mood_archive_db"
    }
}
