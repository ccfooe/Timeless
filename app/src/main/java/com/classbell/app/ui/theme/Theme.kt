package com.classbell.app.ui.theme

import android.app.Activity
import android.os.Build
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
    primary = PinkPrimary,
    onPrimary = Color.White,
    primaryContainer = PinkSurface,
    onPrimaryContainer = PinkDark,
    secondary = PinkLight,
    onSecondary = Color.White,
    background = PinkBg,
    onBackground = TextPrimaryLight,
    surface = Color.White,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFFDECEF),
    onSurfaceVariant = TextSecondaryLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PinkLight,
    onPrimary = Color(0xFF59001A),
    primaryContainer = Color(0xFF8F002E),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = PinkPrimary,
    onSecondary = Color.White,
    background = Color(0xFF141218),
    onBackground = TextPrimaryDark,
    surface = Color(0xFF1E1E2E),
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF2B2830),
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
