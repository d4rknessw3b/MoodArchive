package com.moodarchive.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
