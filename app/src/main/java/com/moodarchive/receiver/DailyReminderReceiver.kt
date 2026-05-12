package com.moodarchive.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.moodarchive.util.NotificationHelper
import com.moodarchive.util.ReminderScheduler

/**
 * BroadcastReceiver для ежедневных напоминаний о ведении дневника.
 *
 * Обрабатывает два сценария:
 *
 * 1. [DAILY_REMINDER] — плановое срабатывание будильника:
 *    - Показывает push-уведомление.
 *    - Самостоятельно ставит следующий будильник через 24 часа,
 *      потому что [ReminderScheduler] использует однократный
 *      setExactAndAllowWhileIdle вместо setRepeating.
 *
 * 2. [BOOT_COMPLETED / LOCKED_BOOT_COMPLETED] — устройство перезагружено:
 *    - Все AlarmManager-будильники сбрасываются при выключении питания,
 *      поэтому нужно их восстановить.
 *    - Час и минута берутся из SharedPreferences (быстро, без корутин),
 *      т.к. goAsync() / coroutine в BroadcastReceiver ненадёжны.
 */
class DailyReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val PREFS_NAME = "reminder_prefs"
        private const val PREF_ENABLED = "reminder_enabled"
        private const val PREF_HOUR = "reminder_hour"
        private const val PREF_MINUTE = "reminder_minute"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.moodarchive.DAILY_REMINDER" -> handleDailyReminder(context, intent)
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED" -> handleBootCompleted(context)
        }
    }

    /**
     * Плановое напоминание: показываем уведомление и перепланируем
     * следующее на то же время завтра.
     */
    private fun handleDailyReminder(context: Context, intent: Intent) {
        // Показываем уведомление
        NotificationHelper.showDailyReminder(context)

        // Извлекаем время из Intent (туда положил ReminderScheduler)
        val hour = intent.getIntExtra(ReminderScheduler.EXTRA_HOUR, -1)
        val minute = intent.getIntExtra(ReminderScheduler.EXTRA_MINUTE, -1)

        if (hour >= 0 && minute >= 0) {
            // Самоперепланирование: ставим следующий будильник
            ReminderScheduler.schedule(context, hour, minute)
        }
    }

    /**
     * Восстанавливаем будильник после перезагрузки устройства.
     * DataStore использует coroutines — недоступен здесь без goAsync().
     * Используем обычный SharedPreferences как быстрый fallback-хранилище.
     *
     * Важно: SettingsViewModel при каждом изменении времени/включении
     * напоминания также сохраняет значения в [PREFS_NAME] через
     * [saveReminderToSharedPrefs] ниже.
     */
    private fun handleBootCompleted(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(PREF_ENABLED, false)
        if (enabled) {
            val hour = prefs.getInt(PREF_HOUR, 21)
            val minute = prefs.getInt(PREF_MINUTE, 0)
            ReminderScheduler.schedule(context, hour, minute)
        }
    }

    /**
     * Утилита для сохранения настроек напоминания в SharedPreferences.
     * Вызывается из SettingsViewModel при изменении состояния, чтобы
     * [handleBootCompleted] мог восстановить расписание без DataStore.
     */
    fun saveReminderToSharedPrefs(
        context: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_ENABLED, enabled)
            .putInt(PREF_HOUR, hour)
            .putInt(PREF_MINUTE, minute)
            .apply()
    }
}
