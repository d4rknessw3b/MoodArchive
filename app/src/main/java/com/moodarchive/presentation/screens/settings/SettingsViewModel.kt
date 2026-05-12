package com.moodarchive.presentation.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moodarchive.domain.repository.AuthRepository
import com.moodarchive.domain.repository.DiaryRepository
import com.moodarchive.domain.repository.SettingsRepository
import com.moodarchive.receiver.DailyReminderReceiver
import com.moodarchive.util.ExportUtils
import com.moodarchive.util.NotificationHelper
import com.moodarchive.util.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val isDarkTheme: Boolean = false,
    val isAutoSync: Boolean = true,
    val pinCode: String = "",
    val isBiometric: Boolean = false,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val isReminderEnabled: Boolean = false,
    val reminderTime: String = "21:00"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val diaryRepository: DiaryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.isDarkTheme.collect { isDark ->
                _uiState.value = _uiState.value.copy(isDarkTheme = isDark)
            }
        }
        viewModelScope.launch {
            settingsRepository.isAutoSyncEnabled.collect { isSync ->
                _uiState.value = _uiState.value.copy(isAutoSync = isSync)
            }
        }
        viewModelScope.launch {
            settingsRepository.pinCode.collect { pin ->
                _uiState.value = _uiState.value.copy(pinCode = pin)
            }
        }
        viewModelScope.launch {
            settingsRepository.isBiometricEnabled.collect { isBio ->
                _uiState.value = _uiState.value.copy(isBiometric = isBio)
            }
        }
        viewModelScope.launch {
            settingsRepository.isReminderEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isReminderEnabled = enabled)
            }
        }
        viewModelScope.launch {
            settingsRepository.reminderTime.collect { time ->
                _uiState.value = _uiState.value.copy(reminderTime = time)
            }
        }
    }

    fun toggleDarkTheme() {
        viewModelScope.launch {
            val newValue = !_uiState.value.isDarkTheme
            settingsRepository.setDarkTheme(newValue)
        }
    }

    fun toggleAutoSync() {
        viewModelScope.launch {
            val newValue = !_uiState.value.isAutoSync
            settingsRepository.setAutoSync(newValue)
        }
    }

    fun setPinCode(pin: String) {
        viewModelScope.launch {
            settingsRepository.setPinCode(pin)
        }
    }

    fun toggleBiometric() {
        viewModelScope.launch {
            val newValue = !_uiState.value.isBiometric
            settingsRepository.setBiometricEnabled(newValue)
        }
    }

    /**
     * Включает или отключает ежедневные уведомления.
     * При включении планирует будильник, при выключении — отменяет.
     */
    fun toggleReminder(context: Context) {
        viewModelScope.launch {
            val newEnabled = !_uiState.value.isReminderEnabled
            settingsRepository.setReminderEnabled(newEnabled)
            val (hour, minute) = parseTime(_uiState.value.reminderTime)
            // Сохраняем в SharedPreferences для восстановления после перезагрузки
            DailyReminderReceiver().saveReminderToSharedPrefs(context, newEnabled, hour, minute)
            if (newEnabled) {
                ReminderScheduler.schedule(context, hour, minute)
                _uiState.value = _uiState.value.copy(
                    syncMessage = "Напоминание включено в ${_uiState.value.reminderTime} 🔔"
                )
            } else {
                ReminderScheduler.cancel(context)
                _uiState.value = _uiState.value.copy(syncMessage = "Напоминание отключено")
            }
        }
    }

    /**
     * Устанавливает новое время уведомления и перепланирует будильник если включён.
     * @param time строка в формате "HH:MM"
     */
    fun setReminderTime(context: Context, time: String) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(time)
            val (hour, minute) = parseTime(time)
            // Обновляем SharedPreferences для восстановления после ребута
            DailyReminderReceiver().saveReminderToSharedPrefs(
                context,
                _uiState.value.isReminderEnabled,
                hour,
                minute
            )
            if (_uiState.value.isReminderEnabled) {
                ReminderScheduler.schedule(context, hour, minute)
                _uiState.value = _uiState.value.copy(
                    syncMessage = "Время напоминания обновлено: $time"
                )
            }
        }
    }

    /**
     * Отправляет тестовое уведомление немедленно для проверки.
     */
    fun sendTestNotification(context: Context) {
        NotificationHelper.showDailyReminder(context)
        _uiState.value = _uiState.value.copy(syncMessage = "Тестовое уведомление отправлено 🔔")
    }

    fun syncNow() {
        _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = null)
        viewModelScope.launch {
            try {
                diaryRepository.syncPendingEntries()
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncMessage = "Синхронизация завершена"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncMessage = "Ошибка: ${e.message}"
                )
            }
        }
    }

    fun clearSyncMessage() {
        _uiState.value = _uiState.value.copy(syncMessage = null)
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onDone()
        }
    }

    fun exportToPdf(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entries = diaryRepository.getAllEntriesOnce()
                withContext(Dispatchers.Main) {
                    ExportUtils.exportToPdf(context, entries)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(syncMessage = "Ошибка экспорта: ${e.message}")
            }
        }
    }

    fun exportToMarkdown(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entries = diaryRepository.getAllEntriesOnce()
                withContext(Dispatchers.Main) {
                    ExportUtils.exportToMarkdown(context, entries)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(syncMessage = "Ошибка экспорта: ${e.message}")
            }
        }
    }

    /** Парсит строку "HH:MM" в пару (hour, minute) */
    private fun parseTime(time: String): Pair<Int, Int> {
        val parts = time.split(":")
        return if (parts.size == 2) {
            Pair(parts[0].toIntOrNull() ?: 21, parts[1].toIntOrNull() ?: 0)
        } else {
            Pair(21, 0)
        }
    }
}
