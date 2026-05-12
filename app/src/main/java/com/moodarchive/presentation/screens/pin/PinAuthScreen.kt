package com.moodarchive.presentation.screens.pin

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Экран аутентификации по PIN-коду с опциональной биометрией.
 */
@Composable
fun PinAuthScreen(
    correctPin: String,
    isBiometricEnabled: Boolean,
    onSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Запуск биометрии
    fun launchBiometric() {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // Если пользователь отменил — просто остаёмся на PIN экране
            }
            override fun onAuthenticationFailed() {
                // Неверный биометрический ввод — ничего не делаем, системный UI покажет ошибку
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("MoodArchive")
            .setSubtitle("Войдите с помощью биометрии")
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                } else {
                    setNegativeButtonText("Использовать PIN")
                }
            }
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }

    // Проверяем доступность биометрии
    val biometricAvailable = remember {
        val bm = BiometricManager.from(context)
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            bm.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        } else {
            bm.canAuthenticate(BIOMETRIC_STRONG)
        }
        result == BiometricManager.BIOMETRIC_SUCCESS
    }

    // Автоматически запустить биометрию при открытии, если включена
    LaunchedEffect(Unit) {
        if (isBiometricEnabled && biometricAvailable) {
            launchBiometric()
        }
    }

    // Проверка введённого PIN
    LaunchedEffect(pin) {
        if (correctPin.isNotEmpty() && pin.length == correctPin.length) {
            if (pin == correctPin) {
                onSuccess()
            } else {
                isError = true
                pin = ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "MoodArchive",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Введите PIN-код",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Индикаторы PIN
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            val pinLen = correctPin.length.coerceAtLeast(4)
            for (i in 0 until pinLen) {
                val isFilled = i < pin.length
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(
                            color = when {
                                isError -> MaterialTheme.colorScheme.error
                                isFilled -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            },
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible = isError, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = "Неверный PIN-код",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Цифровая клавиатура
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9")
            ).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    row.forEach { number ->
                        PinButton(number) {
                            if (pin.length < correctPin.length.coerceAtLeast(4)) {
                                pin += number
                                isError = false
                            }
                        }
                    }
                }
            }

            // Нижний ряд: биометрия / пусто | 0 | backspace
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isBiometricEnabled && biometricAvailable) {
                    IconButton(
                        onClick = { launchBiometric() },
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Биометрия",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(72.dp))
                }

                PinButton("0") {
                    if (pin.length < correctPin.length.coerceAtLeast(4)) {
                        pin += "0"
                        isError = false
                    }
                }

                IconButton(
                    onClick = {
                        if (pin.isNotEmpty()) {
                            pin = pin.dropLast(1)
                            isError = false
                        }
                    },
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Удалить",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun PinButton(number: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(text = number, style = MaterialTheme.typography.headlineMedium)
    }
}
