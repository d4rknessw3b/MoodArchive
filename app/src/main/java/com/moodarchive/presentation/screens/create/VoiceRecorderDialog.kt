package com.moodarchive.presentation.screens.create

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Диалог записи голосовой заметки с визуализацией уровня звука.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceRecorderDialog(
    onDismiss: () -> Unit,
    onRecordingComplete: (Uri, String) -> Unit
) {
    val context = LocalContext.current
    val audioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    var amplitude by remember { mutableFloatStateOf(0f) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var recordingDone by remember { mutableStateOf(false) }

    // Опрос амплитуды
    LaunchedEffect(isRecording) {
        while (isRecording) {
            val maxAmplitude = recorder?.maxAmplitude ?: 0
            amplitude = (maxAmplitude / 32767f).coerceIn(0f, 1f)
            elapsedSeconds++
            delay(1000)
        }
    }

    // Освобождаем ресурсы при закрытии
    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) {
                try {
                    recorder?.stop()
                    recorder?.release()
                } catch (e: Exception) { /* ignore */ }
            }
        }
    }

    fun startRecording() {
        val file = File(
            context.filesDir,
            "voice_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.m4a"
        )
        outputFile = file

        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mr.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mr
        isRecording = true
        elapsedSeconds = 0
    }

    fun stopRecording(): File? {
        return try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            isRecording = false
            recordingDone = true
            outputFile
        } catch (e: Exception) {
            null
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (isRecording) stopRecording()
            onDismiss()
        },
        title = { Text("Голосовая заметка", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!audioPermission.status.isGranted) {
                    Text(
                        "Для записи голоса необходимо разрешение на использование микрофона.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { audioPermission.launchPermissionRequest() }) {
                        Text("Разрешить доступ")
                    }
                } else {
                    // Визуализация уровня звука
                    VoiceWaveform(
                        amplitude = amplitude,
                        isRecording = isRecording
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Таймер
                    val minutes = elapsedSeconds / 60
                    val seconds = elapsedSeconds % 60
                    Text(
                        text = "%02d:%02d".format(minutes, seconds),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when {
                            recordingDone -> "✅ Запись завершена"
                            isRecording -> "🔴 Идёт запись..."
                            else -> "Нажмите, чтобы начать запись"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        },
        confirmButton = {
            if (audioPermission.status.isGranted) {
                if (!recordingDone) {
                    Button(
                        onClick = {
                            if (isRecording) {
                                stopRecording()
                            } else {
                                startRecording()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isRecording) "Стоп" else "Записать")
                    }
                } else {
                    Button(onClick = {
                        outputFile?.let { file ->
                            onRecordingComplete(Uri.fromFile(file), file.name)
                        }
                        onDismiss()
                    }) {
                        Text("Прикрепить")
                    }
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = {
                if (isRecording) stopRecording()
                onDismiss()
            }) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun VoiceWaveform(
    amplitude: Float,
    isRecording: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1f + amplitude * 0.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.height(120.dp)
    ) {
        // Внешние кольца
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size((80 + amplitude * 60).dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .size((60 + amplitude * 40).dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            )
        }

        // Основной круг с иконкой
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    if (isRecording) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Микрофон",
                tint = if (isRecording) Color.White
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
