package com.classbell.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.classbell.app.R

// Curated Master Accent Palettes (Bauhaus, Apple & Modernist)
enum class AppAccentColor(
    val key: String,
    val titleRes: Int,
    val primary: Color,
    val secondary: Color,
    val subtle: Color,
    val lightContainer: Color,
    val hexCode: String
) {
    COBALT(
        key = "blue",
        titleRes = R.string.accent_cobalt,
        primary = Color(0xFF007AFF),
        secondary = Color(0xFF5AC8FA),
        subtle = Color(0x24007AFF),
        lightContainer = Color(0xFFE5F1FF),
        hexCode = "#007AFF"
    ),
    EMERALD(
        key = "green",
        titleRes = R.string.accent_emerald,
        primary = Color(0xFF34C759),
        secondary = Color(0xFF30B0C7),
        subtle = Color(0x2434C759),
        lightContainer = Color(0xFFE8F9ED),
        hexCode = "#34C759"
    ),
    IRIS(
        key = "purple",
        titleRes = R.string.accent_iris,
        primary = Color(0xFFAF52DE),
        secondary = Color(0xFF5856D6),
        subtle = Color(0x24AF52DE),
        lightContainer = Color(0xFFF6EAFE),
        hexCode = "#AF52DE"
    ),
    AMBER(
        key = "amber",
        titleRes = R.string.accent_amber,
        primary = Color(0xFFFF9500),
        secondary = Color(0xFFFFCC00),
        subtle = Color(0x24FF9500),
        lightContainer = Color(0xFFFFF6E5),
        hexCode = "#FF9500"
    ),
    ORANGE(
        key = "orange",
        titleRes = R.string.accent_orange,
        primary = Color(0xFFFF6B35),
        secondary = Color(0xFFFF8A00),
        subtle = Color(0x24FF6B35),
        lightContainer = Color(0xFFFFEFE9),
        hexCode = "#FF6B35"
    );

    companion object {
        fun fromKey(key: String): AppAccentColor {
            return entries.firstOrNull { it.key == key } ?: COBALT
        }
    }
}

// Apple-Grade Sunset Coral / Amber Palette (Inspired by Reference Screenshot)
val AccentOrange = Color(0xFFFF6B35)
val AccentAmber = Color(0xFFFF8A00)
val AccentOrangeLight = Color(0xFFFFA94D)
val AccentOrangeSubtle = Color(0x24FF6B35)

// Compatibility aliases
val PinkPrimary = AccentOrange
val PinkDark = AccentAmber
val CreamBackground = Color(0xFFF7F8FA)

// Glassmorphism Surfaces & Specular Highlights
val GlassWhite = Color(0xCCFFFFFF)           // ~80% frosted translucent white
val GlassWhiteStroke = Color(0x99FFFFFF)     // Crisp specular rim
val GlassDark = Color(0xD91C1C24)            // Ultra-dark obsidian frosted glass
val GlassDarkStroke = Color(0x33FFFFFF)      // Subtle specular rim for dark mode

// Premium Typography
val TextPrimaryLight = Color(0xFF1C1C1E)
val TextSecondaryLight = Color(0xFF8E8E93)
val TextTertiaryLight = Color(0xFFAEB5BC)

val TextPrimaryDark = Color(0xFFF2F2F7)
val TextSecondaryDark = Color(0xFF98989D)
val TextTertiaryDark = Color(0xFF636366)

// Functional System Colors
val SystemGreen = Color(0xFF34C759)
val SystemBlue = Color(0xFF007AFF)
val SystemPurple = Color(0xFFAF52DE)
val SystemRed = Color(0xFFFF3B30)
val SystemTeal = Color(0xFF30B0C7)

// Course Color Palette (Rich iOS-style tints)
val CourseColors = listOf(
    "#FF6B35", // Sunset Coral
    "#007AFF", // Cobalt Blue
    "#AF52DE", // Electric Violet
    "#34C759", // Emerald Green
    "#FF9500", // Warm Amber
    "#30B0C7", // Cyan Teal
    "#FF2D55", // Neon Pink
    "#5856D6"  // Deep Indigo
)
