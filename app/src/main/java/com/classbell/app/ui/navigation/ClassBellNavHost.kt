package com.classbell.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.material3.Scaffold
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.classbell.app.ClassBellApp
import com.classbell.app.R
import com.classbell.app.ui.calendar.CalendarScreen
import com.classbell.app.ui.calendar.CalendarViewModel
import com.classbell.app.ui.import.ImportConfirmScreen
import com.classbell.app.ui.import.ImportViewModel
import com.classbell.app.ui.schedule.ScheduleScreen
import com.classbell.app.ui.schedule.ScheduleViewModel
import com.classbell.app.ui.settings.AboutScreen
import com.classbell.app.ui.settings.AppearanceSettingsScreen
import com.classbell.app.ui.settings.LlmSettingsScreen
import com.classbell.app.ui.settings.PeriodTableEditorScreen
import com.classbell.app.ui.settings.SettingsScreen
import com.classbell.app.ui.settings.SettingsViewModel
import com.classbell.app.ui.settings.SurvivalGuideScreen
import com.classbell.app.ui.settings.UserGuideScreen
import com.classbell.app.ui.stats.StatsScreen
import com.classbell.app.ui.stats.StatsViewModel
import com.classbell.app.ui.theme.AccentOrange
import com.classbell.app.ui.theme.AccentOrangeSubtle
import com.classbell.app.ui.theme.LocalAccentColor
import com.classbell.app.ui.today.TodayScreen
import com.classbell.app.ui.today.TodayViewModel

private val TabOrder = listOf(
    Screen.Today.route,
    Screen.Schedule.route,
    Screen.Calendar.route,
    Screen.Stats.route,
    Screen.Settings.route
)

private fun getTabOrder(route: String?): Int {
    val idx = TabOrder.indexOf(route)
    return if (idx >= 0) idx else 0
}

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
            context,
            app.scheduleRepository,
            app.affairRepository,
            app.settingsRepository,
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
            app.scheduleRepository,
            app.periodRepository
        )
    )

    val isTopLevelDestination = BottomNavItems.any { it.route == currentRoute }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevelDestination) {
                // Native Apple/Smartisan flush-to-bottom TabBar with ultra-fine hairline divider
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(58.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
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

                            val accentColor = LocalAccentColor.current
                            val targetColor = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            val animatedItemColor by animateColorAsState(
                                targetValue = targetColor,
                                animationSpec = tween(200, easing = LinearOutSlowInEasing),
                                label = "tabItemColor"
                            )
                            val iconScale by animateFloatAsState(
                                targetValue = if (selected) 1.08f else 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "tabIconScale"
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                screen.icon?.let {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = localizedTitle,
                                        tint = animatedItemColor,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .graphicsLayer {
                                                scaleX = iconScale
                                                scaleY = iconScale
                                            }
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = localizedTitle,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = animatedItemColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(
                top = if (isTopLevelDestination) innerPadding.calculateTopPadding() else 0.dp,
                bottom = innerPadding.calculateBottomPadding()
            ),
            enterTransition = {
                val initialIdx = getTabOrder(initialState.destination.route)
                val targetIdx = getTabOrder(targetState.destination.route)
                val sign = if (targetIdx >= initialIdx) 1 else -1

                slideInHorizontally(
                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f),
                    initialOffsetX = { fullWidth -> (fullWidth * 0.08f * sign).toInt() }
                ) + fadeIn(
                    animationSpec = tween(220, easing = LinearOutSlowInEasing)
                )
            },
            exitTransition = {
                val initialIdx = getTabOrder(initialState.destination.route)
                val targetIdx = getTabOrder(targetState.destination.route)
                val sign = if (targetIdx >= initialIdx) -1 else 1

                slideOutHorizontally(
                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f),
                    targetOffsetX = { fullWidth -> (fullWidth * 0.08f * sign).toInt() }
                ) + fadeOut(
                    animationSpec = tween(160, easing = FastOutLinearInEasing)
                )
            },
            popEnterTransition = {
                val initialIdx = getTabOrder(initialState.destination.route)
                val targetIdx = getTabOrder(targetState.destination.route)
                val sign = if (targetIdx >= initialIdx) 1 else -1

                slideInHorizontally(
                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f),
                    initialOffsetX = { fullWidth -> (fullWidth * 0.08f * sign).toInt() }
                ) + fadeIn(
                    animationSpec = tween(220, easing = LinearOutSlowInEasing)
                )
            },
            popExitTransition = {
                val initialIdx = getTabOrder(initialState.destination.route)
                val targetIdx = getTabOrder(targetState.destination.route)
                val sign = if (targetIdx >= initialIdx) -1 else 1

                slideOutHorizontally(
                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f),
                    targetOffsetX = { fullWidth -> (fullWidth * 0.08f * sign).toInt() }
                ) + fadeOut(
                    animationSpec = tween(160, easing = FastOutLinearInEasing)
                )
            }
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
                    onNavigateToImportConfirm = { navController.navigate("import_confirm") },
                    onNavigateToUserGuide = { navController.navigate(Screen.UserGuide.route) },
                    onNavigateToAppearance = { tab -> navController.navigate(Screen.Appearance.createRoute(tab)) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) },
                    onNavigateToSurvivalGuide = { navController.navigate(Screen.SurvivalGuide.route) },
                    onNavigateToLlmSettings = { navController.navigate(Screen.LlmSettings.route) }
                )
            }

            composable(
                route = Screen.Appearance.route,
                arguments = listOf(
                    navArgument("tab") {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                )
            ) { backStackEntry ->
                val tab = backStackEntry.arguments?.getInt("tab") ?: 0
                AppearanceSettingsScreen(
                    viewModel = settingsViewModel,
                    initialTab = tab,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.About.route) {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SurvivalGuide.route) {
                SurvivalGuideScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.LlmSettings.route) {
                LlmSettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.PeriodTableEditor.route) {
                PeriodTableEditorScreen(
                    periodRepository = app.periodRepository,
                    onNavigateBack = { navController.popBackStack() },
                    onImportPeriodTable = { uri -> importViewModel.parsePeriodTableImage(uri) },
                    onNavigateToImportConfirm = { navController.navigate("import_confirm") }
                )
            }

            composable(Screen.UserGuide.route) {
                UserGuideScreen(
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
