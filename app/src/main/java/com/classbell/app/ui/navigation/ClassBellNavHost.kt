package com.classbell.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.classbell.app.ClassBellApp
import com.classbell.app.R
import com.classbell.app.ui.calendar.CalendarScreen
import com.classbell.app.ui.calendar.CalendarViewModel
import com.classbell.app.ui.import.ImportConfirmScreen
import com.classbell.app.ui.import.ImportViewModel
import com.classbell.app.ui.schedule.ScheduleScreen
import com.classbell.app.ui.schedule.ScheduleViewModel
import com.classbell.app.ui.settings.PeriodTableEditorScreen
import com.classbell.app.ui.settings.SettingsScreen
import com.classbell.app.ui.settings.SettingsViewModel
import com.classbell.app.ui.stats.StatsScreen
import com.classbell.app.ui.stats.StatsViewModel
import com.classbell.app.ui.theme.AccentOrange
import com.classbell.app.ui.theme.AccentOrangeSubtle
import com.classbell.app.ui.today.TodayScreen
import com.classbell.app.ui.today.TodayViewModel

@Composable
fun ClassBellNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    val app = context.applicationContext as ClassBellApp

    // ViewModels with updated factories
    val todayViewModel: TodayViewModel = viewModel(
        factory = TodayViewModel.Factory(
            context,
            app.scheduleRepository,
            app.courseRepository,
            app.affairRepository,
            app.settingsRepository,
            app.alarmScheduler
        )
    )

    val scheduleViewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModel.Factory(
            app.scheduleRepository,
            app.courseRepository
        )
    )

    val calendarViewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModel.Factory(
            app.scheduleRepository,
            app.affairRepository,
            app.alarmScheduler
        )
    )

    val statsViewModel: StatsViewModel = viewModel(
        factory = StatsViewModel.Factory(
            app.scheduleRepository
        )
    )

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            app.settingsRepository,
            app.semesterRepository,
            app.periodRepository,
            app.backupManager
        )
    )

    val importViewModel: ImportViewModel = viewModel(
        factory = ImportViewModel.Factory(
            context,
            app.settingsRepository,
            app.courseRepository,
            app.semesterRepository,
            app.scheduleRepository
        )
    )

    val isTopLevelDestination = BottomNavItems.any { it.route == currentRoute }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevelDestination) {
                // Floating iOS/ColorOS 16 Frosted Glass Dock Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(26.dp))
                            .clip(RoundedCornerShape(26.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(26.dp)),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp
                    ) {
                        BottomNavItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            val localizedTitle = when (screen) {
                                Screen.Today -> stringResource(R.string.tab_today)
                                Screen.Schedule -> stringResource(R.string.tab_schedule)
                                Screen.Calendar -> stringResource(R.string.tab_calendar)
                                Screen.Stats -> stringResource(R.string.tab_stats)
                                Screen.Settings -> stringResource(R.string.tab_settings)
                                else -> screen.title
                            }

                            NavigationBarItem(
                                icon = {
                                    screen.icon?.let {
                                        Icon(imageVector = it, contentDescription = localizedTitle)
                                    }
                                },
                                label = { Text(localizedTitle, style = MaterialTheme.typography.labelSmall) },
                                selected = selected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AccentOrange,
                                    selectedTextColor = AccentOrange,
                                    indicatorColor = AccentOrangeSubtle,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Today.route) {
                TodayScreen(viewModel = todayViewModel)
            }

            composable(Screen.Schedule.route) {
                ScheduleScreen(viewModel = scheduleViewModel)
            }

            composable(Screen.Calendar.route) {
                CalendarScreen(viewModel = calendarViewModel)
            }

            composable(Screen.Stats.route) {
                StatsScreen(viewModel = statsViewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    importViewModel = importViewModel,
                    onNavigateToPeriodEditor = { navController.navigate(Screen.PeriodTableEditor.route) },
                    onNavigateToImportConfirm = { navController.navigate("import_confirm") }
                )
            }

            composable(Screen.PeriodTableEditor.route) {
                PeriodTableEditorScreen(
                    periodRepository = app.periodRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("import_confirm") {
                ImportConfirmScreen(
                    viewModel = importViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
