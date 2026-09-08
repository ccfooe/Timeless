package com.classbell.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAccentColor = compositionLocalOf { AppAccentColor.COBALT.primary }
val LocalAccentColorSubtle = compositionLocalOf { AppAccentColor.COBALT.subtle }

@Composable
fun ClassBellTheme(
    themeMode: String = "system",
    accentKey: String = "blue",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val accent = AppAccentColor.fromKey(accentKey)

    val lightScheme = lightColorScheme(
        primary = accent.primary,
        onPrimary = Color.White,
        primaryContainer = accent.lightContainer,
        onPrimaryContainer = accent.primary,
        secondary = accent.secondary,
        onSecondary = Color.White,
        background = Color(0xFFF7F8FA),
        onBackground = TextPrimaryLight,
        surface = Color.White,
        onSurface = TextPrimaryLight,
        surfaceVariant = Color(0xFFF0F2F5),
        onSurfaceVariant = TextSecondaryLight
    )

    val darkScheme = darkColorScheme(
        primary = accent.primary,
        onPrimary = Color.White,
        primaryContainer = accent.subtle,
        onPrimaryContainer = accent.secondary,
        secondary = accent.secondary,
        onSecondary = Color.White,
        background = Color(0xFF0F0F13),
        onBackground = TextPrimaryDark,
        surface = Color(0xFF161619),
        onSurface = TextPrimaryDark,
        surfaceVariant = Color(0xFF222228),
        onSurfaceVariant = TextSecondaryDark
    )

    val colorScheme = if (darkTheme) darkScheme else lightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalAccentColor provides accent.primary,
        LocalAccentColorSubtle provides accent.subtle
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
