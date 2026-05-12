package com.moodarchive.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.moodarchive.receiver.DailyReminderReceiver
import java.util.Calendar

/**
 * Планировщик ежедневных напоминаний через AlarmManager.
 *
 * Использует setExactAndAllowWhileIdle вместо setRepeating, потому что:
 * - setRepeating начиная с API 19 является неточным (Doze mode сдвигает его).
 * - setExactAndAllowWhileIdle гарантирует срабатывание даже в Doze.
 * - После каждого срабатывания DailyReminderReceiver самостоятельно
 *   ставит следующий будильник через этот же планировщик.
 *
 * Время (час и минута) передаётся через Intent в extras, чтобы
 * receiver мог перепланироваться без обращения к DataStore.
 */
object ReminderScheduler {

    private const val REQUEST_CODE = 2001
    const val EXTRA_HOUR = "extra_hour"
    const val EXTRA_MINUTE = "extra_minute"

    /**
     * Планирует первое (или обновлённое) ежедневное уведомление.
     * @param hour   час срабатывания (0–23)
     * @param minute минута срабатывания (0–59)
     */
    fun schedule(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Вычисляем ближайший момент срабатывания
        val triggerAt = nextTriggerMillis(hour, minute)
        val pendingIntent = buildPendingIntent(context, hour, minute)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                // Fallback: inexact — лучше чем ничего
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }

    /**
     * Отменяет ежедневное уведомление.
     * Час и минута не важны для отмены — PendingIntent идентифицируется
     * только по REQUEST_CODE и action Intent.
     */
    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, 0, 0)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * Проверяет, активен ли будильник.
     */
    fun isScheduled(context: Context): Boolean {
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, DailyReminderReceiver::class.java).apply {
                action = "com.moodarchive.DAILY_REMINDER"
            },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null
    }

    /**
     * Вычисляет timestamp следующего срабатывания в миллисекундах.
     * Если указанное время сегодня уже прошло — возвращает завтра.
     */
    fun nextTriggerMillis(hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis
    }

    private fun buildPendingIntent(context: Context, hour: Int, minute: Int): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = "com.moodarchive.DAILY_REMINDER"
            // Передаём время, чтобы receiver перепланировал следующий будильник
            putExtra(EXTRA_HOUR, hour)
            putExtra(EXTRA_MINUTE, minute)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
