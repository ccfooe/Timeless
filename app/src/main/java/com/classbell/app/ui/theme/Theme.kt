package com.classbell.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = AccentOrange,
    onPrimary = Color.White,
    primaryContainer = AccentOrangeSubtle,
    onPrimaryContainer = AccentAmber,
    secondary = AccentAmber,
    onSecondary = Color.White,
    background = Color(0xFFF7F8FA),
    onBackground = TextPrimaryLight,
    surface = Color.White,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = TextSecondaryLight
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentOrangeLight,
    onPrimary = Color(0xFF4D1C05),
    primaryContainer = Color(0xFF7A2E0A),
    onPrimaryContainer = Color(0xFFFFD9C7),
    secondary = AccentOrange,
    onSecondary = Color.White,
    background = Color(0xFF0F0F13),
    onBackground = TextPrimaryDark,
    surface = Color(0xFF1A1A22),
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF262630),
    onSurfaceVariant = TextSecondaryDark
)

@Composable
fun ClassBellTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
