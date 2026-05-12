package com.moodarchive.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.moodarchive.R
import com.moodarchive.presentation.MainActivity

/**
 * Вспомогательный класс для управления уведомлениями MoodArchive.
 *
 * Отвечает за:
 * - Создание канала уведомлений (Android 8+, вызывается при старте из Application).
 * - Отображение ежедневного напоминания с рандомным текстом.
 */
object NotificationHelper {

    const val CHANNEL_ID = "moodarchive_reminders"
    const val NOTIFICATION_ID = 1001

    private val REMINDER_MESSAGES = listOf(
        "Как прошёл ваш день? 📝 Запишите свои мысли.",
        "Время для дневника! Поделитесь своими эмоциями ✨",
        "Не забудьте зафиксировать сегодняшнее настроение 🌟",
        "Ваш дневник ждёт вас. Расскажите, как вы себя чувствуете 💭",
        "Минута для рефлексии: что сегодня запомнилось больше всего? 🌅",
        "Дневник эмоций — маленький шаг к большому самопознанию 🧠",
        "Сделайте паузу и запишите, что вы чувствуете прямо сейчас 💙"
    )

    /**
     * Создаёт канал уведомлений (обязателен для Android 8+, API 26).
     * Должен вызываться как можно раньше — в [Application.onCreate].
     * Повторный вызов безопасен: система игнорирует уже существующий канал.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ежедневные напоминания",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Напоминания о ведении дневника эмоций"
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 300L, 200L, 300L)
                setSound(soundUri, audioAttributes)
                enableLights(true)
                lightColor = Color.parseColor("#7C5CBF") // фирменный фиолетовый
                setShowBadge(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Показывает напоминание о ведении дневника.
     * Нажатие на уведомление открывает главный экран приложения.
     */
    fun showDailyReminder(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // На Android 13+ пользователь может отозвать разрешение POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!manager.areNotificationsEnabled()) return
        }

        // Интент для открытия приложения по нажатию на уведомление
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = REMINDER_MESSAGES.random()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            // ic_notification — монохромная иконка 24dp для статус-бара
            // Если её нет, используем ic_launcher как временный вариант
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("MoodArchive 📔")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            // Цвет акцента иконки (фирменный фиолетовый)
            .setColor(Color.parseColor("#7C5CBF"))
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }
}
