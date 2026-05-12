package com.moodarchive.presentation.screens.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

/** Режим камеры: фото или видео */
enum class CameraMode { PHOTO, VIDEO }

/** Состояние вспышки */
enum class FlashMode {
    OFF, ON, AUTO;

    fun next() = when (this) {
        OFF -> ON
        ON -> AUTO
        AUTO -> OFF
    }

    fun toImageCaptureFlash() = when (this) {
        OFF -> ImageCapture.FLASH_MODE_OFF
        ON -> ImageCapture.FLASH_MODE_ON
        AUTO -> ImageCapture.FLASH_MODE_AUTO
    }
}

/**
 * Экран камеры на основе CameraX.
 *
 * Поддерживает:
 * - Съёмку фотографий (ImageCapture)
 * - Запись видео (VideoCapture + Recorder)
 * - Переключение фронт/тыловая камера
 * - Управление вспышкой (фото-режим)
 * - Таймер записи видео
 * - Запрос разрешений через Accompanist
 *
 * @param mode         начальный режим (PHOTO или VIDEO)
 * @param onPhotoCaptured URI сделанного снимка
 * @param onVideoRecorded URI записанного видео
 * @param onClose       навигация назад
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    mode: CameraMode = CameraMode.PHOTO,
    onPhotoCaptured: (Uri) -> Unit,
    onVideoRecorded: (Uri) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // ─── Разрешения ───────────────────────────────────────────────
    val requiredPermissions = buildList {
        add(Manifest.permission.CAMERA)
        if (mode == CameraMode.VIDEO) {
            add(Manifest.permission.RECORD_AUDIO)
        }
    }
    val permissionsState = rememberMultiplePermissionsState(requiredPermissions)

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // ─── Состояние UI ─────────────────────────────────────────────
    var currentMode by remember { mutableStateOf(mode) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(FlashMode.OFF) }
    var isRecording by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableIntStateOf(0) }

    // ─── CameraX-объекты ──────────────────────────────────────────
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    // ─── Таймер записи ────────────────────────────────────────────
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordSeconds = 0
            while (isRecording) {
                delay(1000)
                recordSeconds++
            }
        } else {
            recordSeconds = 0
        }
    }

    // ─── Инициализация камеры ─────────────────────────────────────
    fun bindCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            cameraProvider.unbindAll()

            when (currentMode) {
                CameraMode.PHOTO -> {
                    val imgCapture = ImageCapture.Builder()
                        .setFlashMode(flashMode.toImageCaptureFlash())
                        .build()
                    imageCapture = imgCapture
                    videoCapture = null
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imgCapture)
                }
                CameraMode.VIDEO -> {
                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build()
                    val vidCapture = VideoCapture.withOutput(recorder)
                    videoCapture = vidCapture
                    imageCapture = null
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, vidCapture)
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(lensFacing, currentMode) {
        if (permissionsState.allPermissionsGranted) bindCamera()
    }

    LaunchedEffect(flashMode) {
        imageCapture?.flashMode = flashMode.toImageCaptureFlash()
    }

    DisposableEffect(Unit) {
        onDispose {
            activeRecording?.stop()
            cameraExecutor.shutdown()
        }
    }

    // ─── Захват фото ──────────────────────────────────────────────
    fun takePhoto() {
        val imgCapture = imageCapture ?: return
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "MOOD_$name.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MoodArchive")
            }
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imgCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    output.savedUri?.let { onPhotoCaptured(it) }
                }
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed", exc)
                }
            }
        )
    }

    // ─── Запись видео ─────────────────────────────────────────────
    fun toggleVideoRecording() {
        val vidCapture = videoCapture ?: return
        if (isRecording) {
            activeRecording?.stop()
            activeRecording = null
            isRecording = false
        } else {
            val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "MOOD_$name.mp4")
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/MoodArchive")
                }
            }
            val outputOptions = MediaStoreOutputOptions.Builder(
                context.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).setContentValues(contentValues).build()

            activeRecording = vidCapture.output
                .prepareRecording(context, outputOptions)
                .apply { withAudioEnabled() }
                .start(ContextCompat.getMainExecutor(context)) { event ->
                    when (event) {
                        is VideoRecordEvent.Finalize -> {
                            if (!event.hasError()) {
                                event.outputResults.outputUri.let { onVideoRecorded(it) }
                            } else {
                                Log.e("CameraX", "Video capture failed: ${event.error}")
                            }
                            isRecording = false
                        }
                        is VideoRecordEvent.Start -> isRecording = true
                        else -> Unit
                    }
                }
        }
    }

    // ─── UI ───────────────────────────────────────────────────────
    if (!permissionsState.allPermissionsGranted) {
        CameraPermissionDeniedScreen(onClose = onClose)
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // Превью камеры
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Верхняя панель
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка назад
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
            }

            // Таймер записи
            if (isRecording) {
                RecordingTimer(seconds = recordSeconds)
            }

            // Кнопка вспышки (только фото)
            if (currentMode == CameraMode.PHOTO && lensFacing == CameraSelector.LENS_FACING_BACK) {
                IconButton(
                    onClick = { flashMode = flashMode.next() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            FlashMode.OFF -> Icons.Default.FlashOff
                            FlashMode.ON -> Icons.Default.FlashOn
                            FlashMode.AUTO -> Icons.Default.FlashAuto
                        },
                        contentDescription = "Вспышка",
                        tint = if (flashMode == FlashMode.ON) Color.Yellow else Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(40.dp))
            }
        }

        // Нижняя панель управления
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Переключатель режима (только когда не идёт запись)
            if (!isRecording) {
                CameraModeSelector(
                    currentMode = currentMode,
                    onModeSelected = { currentMode = it }
                )
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Spacer(modifier = Modifier.height(56.dp))
            }

            // Основная строка: переключатель камеры | спуск затвора | (пусто)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Переключение фронт/тыл
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                            CameraSelector.LENS_FACING_FRONT
                        else
                            CameraSelector.LENS_FACING_BACK
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        Icons.Default.Cameraswitch,
                        contentDescription = "Переключить камеру",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Кнопка спуска затвора / старт-стоп записи
                ShutterButton(
                    mode = currentMode,
                    isRecording = isRecording,
                    onClick = {
                        when (currentMode) {
                            CameraMode.PHOTO -> takePhoto()
                            CameraMode.VIDEO -> toggleVideoRecording()
                        }
                    }
                )

                // Заглушка для симметрии
                Spacer(modifier = Modifier.size(52.dp))
            }
        }
    }
}

/** Кнопка спуска затвора с анимацией */
@Composable
private fun ShutterButton(
    mode: CameraMode,
    isRecording: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shutter_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val innerColor by animateColorAsState(
        targetValue = when {
            mode == CameraMode.VIDEO && isRecording -> Color(0xFFE53935)
            else -> Color.White
        },
        label = "shutter_color"
    )
    val innerShape = if (isRecording) RoundedCornerShape(8.dp) else CircleShape

    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .clip(CircleShape)
            .border(4.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(innerShape)
                .background(innerColor),
            contentAlignment = Alignment.Center
        ) {
            when {
                mode == CameraMode.VIDEO && isRecording -> Icon(
                    Icons.Default.Stop,
                    contentDescription = "Стоп",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                mode == CameraMode.VIDEO -> Icon(
                    Icons.Default.FiberManualRecord,
                    contentDescription = "Запись",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(32.dp)
                )
                else -> Unit
            }
        }
    }
}

/** Переключатель режима фото/видео */
@Composable
private fun CameraModeSelector(
    currentMode: CameraMode,
    onModeSelected: (CameraMode) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CameraMode.entries.forEach { m ->
            val selected = currentMode == m
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onModeSelected(m) }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (m == CameraMode.PHOTO) Icons.Default.PhotoCamera else Icons.Default.Videocam,
                    contentDescription = null,
                    tint = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (m == CameraMode.PHOTO) "Фото" else "Видео",
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

/** Таймер записи */
@Composable
private fun RecordingTimer(seconds: Int) {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    val text = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE53935).copy(alpha = 0.85f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
        Text(text = text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** Заглушка при отказе от разрешений */
@Composable
private fun CameraPermissionDeniedScreen(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Photo,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Нет доступа к камере",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Разрешите доступ в настройках приложения",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = onClose,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
            ) {
                Text("Закрыть", color = Color.White)
            }
        }
    }
}
