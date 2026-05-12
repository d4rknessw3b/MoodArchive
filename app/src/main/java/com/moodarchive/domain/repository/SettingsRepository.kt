package com.moodarchive.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория настроек приложения.
 */
interface SettingsRepository {

    /** Получить текущую тему (true = тёмная) */
    val isDarkTheme: Flow<Boolean>

    /** Установить тему */
    suspend fun setDarkTheme(isDark: Boolean)

    /** Включена ли автосинхронизация */
    val isAutoSyncEnabled: Flow<Boolean>

    /** Установить автосинхронизацию */
    suspend fun setAutoSync(enabled: Boolean)

    /** Получить PIN-код (пустая строка = отключён) */
    val pinCode: Flow<String>

    /** Установить PIN-код */
    suspend fun setPinCode(pin: String)

    /** Получить состояние биометрической аутентификации */
    val isBiometricEnabled: Flow<Boolean>

    /** Установить биометрическую аутентификацию */
    suspend fun setBiometricEnabled(enabled: Boolean)

    /** Включены ли ежедневные уведомления */
    val isReminderEnabled: Flow<Boolean>

    /** Установить активность уведомлений */
    suspend fun setReminderEnabled(enabled: Boolean)

    /** Время уведомления в формате "HH:MM" (например "21:00") */
    val reminderTime: Flow<String>

    /** Установить время уведомления */
    suspend fun setReminderTime(time: String)
}
