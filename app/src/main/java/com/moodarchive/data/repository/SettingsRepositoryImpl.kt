package com.moodarchive.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.moodarchive.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация SettingsRepository через DataStore Preferences.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    companion object {
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_AUTO_SYNC = booleanPreferencesKey("auto_sync")
        private val KEY_PIN_CODE = stringPreferencesKey("pin_code")
        private val KEY_BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        private val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        private val KEY_REMINDER_TIME = stringPreferencesKey("reminder_time")
    }

    override val isDarkTheme: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DARK_THEME] ?: false
    }

    override suspend fun setDarkTheme(isDark: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_DARK_THEME] = isDark }
    }

    override val isAutoSyncEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SYNC] ?: true
    }

    override suspend fun setAutoSync(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_AUTO_SYNC] = enabled }
    }

    override val pinCode: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_PIN_CODE] ?: ""
    }

    override suspend fun setPinCode(pin: String) {
        dataStore.edit { prefs -> prefs[KEY_PIN_CODE] = pin }
    }

    override val isBiometricEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC] ?: false
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_BIOMETRIC] = enabled }
    }

    override val isReminderEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_REMINDER_ENABLED] ?: false
    }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_REMINDER_ENABLED] = enabled }
    }

    override val reminderTime: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_REMINDER_TIME] ?: "21:00"
    }

    override suspend fun setReminderTime(time: String) {
        dataStore.edit { prefs -> prefs[KEY_REMINDER_TIME] = time }
    }
}
