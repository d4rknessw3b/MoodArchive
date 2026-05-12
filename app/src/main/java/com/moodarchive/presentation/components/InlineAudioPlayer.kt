package com.moodarchive.presentation.components

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File

/**
 * Встроенный аудиоплеер для воспроизведения голосовых заметок.
 * Использует MediaPlayer, работает с file:// URI из внутреннего хранилища.
 */
@Composable
fun InlineAudioPlayer(
    audioUri: String,
    fileName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var currentMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    // Инициализация плеера
    fun initPlayer(): MediaPlayer? {
        return try {
            val uri = Uri.parse(audioUri)
            val mp = if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (!file.exists()) { error = "Файл не найден"; return null }
                MediaPlayer().apply { setDataSource(file.absolutePath) }
            } else {
                MediaPlayer.create(context, uri) ?: run { error = "Не удалось загрузить"; return null }
            }
            mp.prepare()
            durationMs = mp.duration
            mp.setOnCompletionListener {
                isPlaying = false
                isFinished = true
                progress = 1f
                currentMs = durationMs
            }
            mp
        } catch (e: Exception) {
            error = "Ошибка: ${e.message}"
            null
        }
    }

    fun play() {
        val mp = mediaPlayer ?: initPlayer()?.also { mediaPlayer = it } ?: return
        if (isFinished) {
            mp.seekTo(0)
            isFinished = false
            progress = 0f
            currentMs = 0
        }
        mp.start()
        isPlaying = true
        error = null
    }

    fun pause() {
        mediaPlayer?.pause()
        isPlaying = false
    }

    // Обновление прогресса каждые 200мс
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val mp = mediaPlayer ?: break
            if (mp.isPlaying) {
                currentMs = mp.currentPosition
                val dur = mp.duration.takeIf { it > 0 } ?: 1
                progress = currentMs.toFloat() / dur
            }
            delay(200)
        }
    }

    // Освобождение ресурсов при уходе с экрана
    DisposableEffect(audioUri) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Пульсация иконки при воспроизведении
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isPlaying) Color(0xFFFF7043).copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Кнопка Play/Pause/Replay
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .scale(if (isPlaying) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        when {
                            isPlaying -> Color(0xFFFF7043)
                            isFinished -> MaterialTheme.colorScheme.primary
                            else -> Color(0xFFFF7043).copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { if (isPlaying) pause() else play() },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = when {
                            isFinished -> Icons.Default.Replay
                            isPlaying -> Icons.Default.Pause
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                        tint = if (isPlaying) Color.White
                        else Color(0xFFFF7043),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName.ifBlank { "Голосовая заметка" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        error != null -> error!!
                        isPlaying -> "Воспроизведение..."
                        isFinished -> "Готово"
                        durationMs > 0 -> formatDuration(durationMs)
                        else -> "Нажмите ▶ для воспроизведения"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        error != null -> MaterialTheme.colorScheme.error
                        isPlaying -> Color(0xFFFF7043)
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    }
                )
            }

            // Таймер
            Text(
                text = "${formatDuration(currentMs)} / ${if (durationMs > 0) formatDuration(durationMs) else "--:--"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }

        // Прогресс-бар
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = Color(0xFFFF7043),
            trackColor = Color(0xFFFF7043).copy(alpha = 0.18f)
        )
    }
}

private fun formatDuration(ms: Int): String {
    val totalSeconds = ms / 1000
    val min = totalSeconds / 60
    val sec = totalSeconds % 60
    return "%d:%02d".format(min, sec)
}
