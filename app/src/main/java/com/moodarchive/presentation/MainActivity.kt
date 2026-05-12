package com.moodarchive.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.moodarchive.domain.repository.AuthRepository
import com.moodarchive.domain.repository.DiaryRepository
import com.moodarchive.domain.repository.SettingsRepository
import com.moodarchive.presentation.navigation.AppNavGraph
import com.moodarchive.presentation.theme.MoodArchiveTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Главная Activity приложения MoodArchive.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var diaryRepository: DiaryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by settingsRepository.isDarkTheme.collectAsState(initial = false)
            val navController = rememberNavController()

            // Запрос разрешения на отправку уведомлений (для Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { /* Здесь можно обработать результат, если нужно */ }
                )

                LaunchedEffect(Unit) {
                    val isGranted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!isGranted) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            MoodArchiveTheme(darkTheme = isDarkTheme) {
                AppNavGraph(
                    navController = navController,
                    authRepository = authRepository,
                    diaryRepository = diaryRepository,
                    settingsRepository = settingsRepository
                )
            }
        }
    }
}
