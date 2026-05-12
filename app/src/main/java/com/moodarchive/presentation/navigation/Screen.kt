package com.moodarchive.presentation.navigation

/**
 * Маршруты навигации приложения.
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object CreateEntry : Screen("create_entry")
    data object EditEntry : Screen("edit_entry/{entryId}") {
        fun createRoute(entryId: String) = "edit_entry/$entryId"
    }
    data object ViewEntry : Screen("view_entry/{entryId}") {
        fun createRoute(entryId: String) = "view_entry/$entryId"
    }
    data object Calendar : Screen("calendar")
    data object Statistics : Screen("statistics")
    data object Settings : Screen("settings")
    data object Search : Screen("search")
    data object PinAuth : Screen("pin_auth")
}
