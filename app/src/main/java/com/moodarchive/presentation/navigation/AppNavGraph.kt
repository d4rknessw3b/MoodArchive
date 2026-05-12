package com.moodarchive.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.moodarchive.domain.repository.AuthRepository
import com.moodarchive.domain.repository.DiaryRepository
import com.moodarchive.domain.repository.SettingsRepository
import com.moodarchive.presentation.screens.auth.AuthScreen
import com.moodarchive.presentation.screens.calendar.CalendarScreen
import com.moodarchive.presentation.screens.camera.CameraMode
import com.moodarchive.presentation.screens.camera.CameraScreen
import com.moodarchive.presentation.screens.create.CreateEntryScreen
import com.moodarchive.presentation.screens.home.HomeScreen
import com.moodarchive.presentation.screens.pin.PinAuthScreen
import com.moodarchive.presentation.screens.search.SearchScreen
import com.moodarchive.presentation.screens.settings.SettingsScreen
import com.moodarchive.presentation.screens.splash.SplashScreen
import com.moodarchive.presentation.screens.statistics.StatisticsScreen
import com.moodarchive.presentation.screens.view.ViewEntryScreen

/**
 * Основной навигационный граф приложения.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    authRepository: AuthRepository,
    diaryRepository: DiaryRepository,
    settingsRepository: SettingsRepository
) {
    val pinCode by settingsRepository.pinCode.collectAsState(initial = "")
    val isBiometricEnabled by settingsRepository.isBiometricEnabled.collectAsState(initial = false)

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // ─── Splash ───────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    val destination = if (authRepository.isSignedIn()) {
                        if (pinCode.isNotEmpty()) Screen.PinAuth.route else Screen.Home.route
                    } else {
                        Screen.Auth.route
                    }

                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ─── Pin Auth ─────────────────────────────────────────
        composable(Screen.PinAuth.route) {
            PinAuthScreen(
                correctPin = pinCode,
                isBiometricEnabled = isBiometricEnabled,
                onSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.PinAuth.route) { inclusive = true }
                    }
                }
            )
        }

        // ─── Auth ─────────────────────────────────────────────
        composable(Screen.Auth.route) {
            AuthScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        // ─── Home ─────────────────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCreate = { navController.navigate(Screen.CreateEntry.route) },
                onNavigateToEntry  = { navController.navigate(Screen.ViewEntry.createRoute(it)) },
                onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                onNavigateToStats  = { navController.navigate(Screen.Statistics.route) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        // ─── Create Entry ──────────────────────────────────────────────────────
        composable(Screen.CreateEntry.route) {
            CreateEntryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCamera = { mode -> navController.navigate(Screen.Camera.route.replace("{mode}", mode)) },
                navController = navController
            )
        }

        // ─── Edit Entry ───────────────────────────────────────────────────────
        composable(
            route = Screen.EditEntry.route,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType })
        ) { backStackEntry ->
            CreateEntryScreen(
                entryId = backStackEntry.arguments?.getString("entryId"),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCamera = { mode -> navController.navigate(Screen.Camera.route.replace("{mode}", mode)) },
                navController = navController
            )
        }

        // ─── View Entry ────────────────────────────────────────
        composable(
            route = Screen.ViewEntry.route,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId") ?: return@composable
            ViewEntryScreen(
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() },
                onDelete = {
                    navController.popBackStack()
                }
            )
        }

        // ─── Calendar ─────────────────────────────────────────
        composable(Screen.Calendar.route) {
            CalendarScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntry = { navController.navigate(Screen.ViewEntry.createRoute(it)) }
            )
        }

        // ─── Statistics ───────────────────────────────────────
        composable(Screen.Statistics.route) {
            StatisticsScreen(onNavigateBack = { navController.popBackStack() })
        }

        // ─── Settings ─────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // ─── Search ───────────────────────────────────────────
        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntry = { navController.navigate(Screen.ViewEntry.createRoute(it)) }
            )
        }

        // ─── Camera ───────────────────────────────────────────────────────
        composable(
            route = Screen.Camera.route,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val modeArg = backStackEntry.arguments?.getString("mode") ?: "photo"
            val cameraMode = if (modeArg == "video") CameraMode.VIDEO else CameraMode.PHOTO
            CameraScreen(
                mode = cameraMode,
                onPhotoCaptured = { uri ->
                    // Возвращаем URI через SavedStateHandle предыдущему экрану
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("captured_photo_uri", uri.toString())
                    navController.popBackStack()
                },
                onVideoRecorded = { uri ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("captured_video_uri", uri.toString())
                    navController.popBackStack()
                },
                onClose = { navController.popBackStack() }
            )
        }
    }
}
