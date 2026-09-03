package com.classbell.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Today : Screen("today", "今日", Icons.Default.Today)
    object Schedule : Screen("schedule", "课表", Icons.Default.DateRange)
    object Calendar : Screen("calendar", "日历", Icons.Default.CalendarMonth)
    object Stats : Screen("stats", "统计", Icons.Default.Insights)
    object Settings : Screen("settings", "我的", Icons.Default.Person)

    // Sub-screens
    object PeriodTableEditor : Screen("period_editor", "作息时间表")
    object ImportTimetable : Screen("import_timetable", "导入课表")
    object ImportCalendar : Screen("import_calendar", "导入校历")
}

val BottomNavItems = listOf(
    Screen.Today,
    Screen.Schedule,
    Screen.Calendar,
    Screen.Stats,
    Screen.Settings
)
