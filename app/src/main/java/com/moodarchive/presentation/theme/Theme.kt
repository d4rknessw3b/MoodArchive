package com.moodarchive.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = MoodPrimaryLight,
    onPrimary = MoodOnPrimary,
    secondary = MoodSecondary,
    onSecondary = MoodOnPrimary,
    tertiary = MoodSecondaryLight,
    background = MoodBackgroundDark,
    onBackground = MoodOnBackgroundDark,
    surface = MoodSurfaceDark,
    onSurface = MoodOnSurfaceDark,
    surfaceVariant = MoodCardDark,
    error = MoodError,
    onError = MoodOnError
)

private val LightColorScheme = lightColorScheme(
    primary = MoodPrimary,
    onPrimary = MoodOnPrimary,
    secondary = MoodSecondary,
    onSecondary = MoodOnPrimary,
    tertiary = MoodSecondaryDark,
    background = MoodBackgroundLight,
    onBackground = MoodOnBackgroundLight,
    surface = MoodSurfaceLight,
    onSurface = MoodOnSurfaceLight,
    surfaceVariant = MoodCardLight,
    error = MoodError,
    onError = MoodOnError
)

@Composable
fun MoodArchiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MoodTypography,
        content = content
    )
}
