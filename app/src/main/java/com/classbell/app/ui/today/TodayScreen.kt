package com.classbell.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classbell.app.R
import com.classbell.app.core.TimeUtils
import com.classbell.app.data.repository.SettingsRepository
import com.classbell.app.data.model.Course
import com.classbell.app.data.model.MeetingOccurrence
import com.classbell.app.data.model.MeetingStatus
import com.classbell.app.ui.calendar.AffairEditDialog
import com.classbell.app.ui.calendar.ShiftCourseDialog
import com.classbell.app.ui.components.PureSurface
import com.classbell.app.ui.import.SmartIngestDialog
import com.classbell.app.ui.import.SmartIngestConfirmDialog
import com.classbell.app.llm.UniversalIngestResult
import com.classbell.app.ui.theme.AccentOrange
import com.classbell.app.ui.theme.AccentOrangeSubtle
import com.classbell.app.ui.theme.LocalAccentColor
import com.classbell.app.ui.theme.LocalAccentColorSubtle

@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accentColor = LocalAccentColor.current
    val accentSubtle = LocalAccentColorSubtle.current
    var showStartClassDialog by remember { mutableStateOf(false) }
    var showAddAffairDialog by remember { mutableStateOf(false) }
    var showShiftDialog by remember { mutableStateOf(false) }
    var showSmartIngestDialog by remember { mutableStateOf(false) }
    var smartIngestResult by remember { mutableStateOf<UniversalIngestResult?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header: Date + Week + Phase Badge + '+' Entrance
            item {
                TodayHeader(
                    weekNumber = state.weekNumber,
                    phase = state.semesterPhase,
                    dateString = TimeUtils.formatHeaderDate(state.currentDate),
                    isHoliday = state.isHolidayToday,
                    holidayName = state.holidayName,
                    hasShiftOverride = state.todayOverrides.isNotEmpty(),
                    onOpenShiftDialog = { showShiftDialog = true },
                    onOpenSmartIngest = { showSmartIngestDialog = true }
                )
            }

            // 2. Live Activity / Now-In-Class Active Card & Today's Tasks
            item {
                NowInClassCard(
                    currentMeeting = state.currentMeeting,
                    nextMeeting = state.nextMeeting,
                    currentTime = state.currentTime,
                    todayAffairs = state.todayAffairs,
                    sessionSuppression = state.sessionSuppression,
                    onToggleAffair = { viewModel.toggleAffair(it) },
                    onAddAffair = { showAddAffairDialog = true },
                    onEndClass = { viewModel.endClassNow() },
                    onEndPeriod = { viewModel.dismissCurrentPeriod() },
                    onEndHalfDay = { viewModel.dismissCurrentHalfDay() },
                    onEndFullDay = { viewModel.dismissFullDay() },
                    onRestoreTiming = { viewModel.restoreTiming() },
                    onExtendClass = { viewModel.extendClass(it) },
                    userRole = state.userRole,
                    autoTimezone = state.autoTimezoneConversion
                )
            }

            // 3. Quick Start Class Hero Banner (Only shown when not currently in class)
            if (state.currentMeeting == null) {
                item {
                    PureSurface(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = 16.dp,
                        onClick = { showStartClassDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (state.userRole == SettingsRepository.ROLE_STUDENT) {
                                        stringResource(R.string.hero_start_class_title_student)
                                    } else {
                                        stringResource(R.string.hero_start_class_title)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.hero_start_class_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 4. Today's Class Schedule Section Title
            item {
                val activeCount = state.todayMeetings.count { !it.isHolidaySuppressed }
                val titleText = if (state.userRole == SettingsRepository.ROLE_STUDENT) {
                    stringResource(R.string.today_title_student)
                } else {
                    stringResource(R.string.today_title_teacher)
                }
                val countText = if (state.isHolidayToday || activeCount == 0) {
                    if (state.holidayName != null) "${state.holidayName} · 今日停课" else "今日无课程"
                } else if (state.userRole == SettingsRepository.ROLE_STUDENT) {
                    stringResource(R.string.today_sub_student, activeCount)
                } else {
                    stringResource(R.string.today_sub_teacher, activeCount)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.todayMeetings.isEmpty()) {
                item {
                    PureSurface(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = 24.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.no_classes_today),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.todayMeetings) { meeting ->
                    TodayMeetingCard(
                        meeting = meeting,
                        userRole = state.userRole,
                        onStartThisClass = {
                            viewModel.startCourseRoutine(meeting)
                        }
                    )
                }
            }
        }

        // Floating Quick Entrance for Smart Ingestion (+ button within easy thumb reach)
        FloatingActionButton(
            onClick = { showSmartIngestDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 96.dp),
            shape = CircleShape,
            containerColor = accentColor,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "智能全能录入",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // Start Class Sheet Dialog
    if (showStartClassDialog) {
        StartClassDialog(
            courses = state.availableCourses,
            onDismiss = { showStartClassDialog = false },
            onStart = { course, duration ->
                viewModel.startClassNow(course, duration)
                showStartClassDialog = false
            }
        )
    }

    // Add Affair Dialog directly from Today screen
    if (showAddAffairDialog) {
        AffairEditDialog(
            initialAffair = null,
            defaultDate = state.currentDate,
            onDismiss = { showAddAffairDialog = false },
            onSave = {
                viewModel.addAffair(it)
                showAddAffairDialog = false
            },
            onSmartParse = { text -> viewModel.parseAffair(text) }
        )
    }

    // Shift Course Dialog from Today Screen 2nd-level menu
    if (showShiftDialog) {
        ShiftCourseDialog(
            targetDate = state.currentDate,
            courses = state.allSemesterCourses,
            currentOverrides = state.todayOverrides,
            onSaveWholeDayShift = { date, follows, note -> viewModel.saveWholeDayShift(date, follows, note) },
            onSaveWholeDayCancel = { date, note -> viewModel.saveWholeDayCancel(date, note) },
            onSaveSingleAdjustment = { date, cId, pStart, pEnd, room, note -> viewModel.saveSingleAdjustment(date, cId, pStart, pEnd, room, note) },
            onSaveSingleCancel = { date, cId, note -> viewModel.saveSingleCancel(date, cId, note) },
            onClearOverrides = { date -> viewModel.clearOverridesForDate(date) },
            onDismiss = { showShiftDialog = false }
        )
    }

    // Smart Ingestion Input Sheet/Dialog
    if (showSmartIngestDialog) {
        SmartIngestDialog(
            referenceDate = state.currentDate,
            onDismiss = { showSmartIngestDialog = false },
            onParsedSuccess = { result ->
                showSmartIngestDialog = false
                smartIngestResult = result
            }
        )
    }

    // Smart Ingestion Confirmation & Editing Dialog
    smartIngestResult?.let { result ->
        SmartIngestConfirmDialog(
            result = result,
            activeSemesterId = state.activeSemester?.id ?: 1L,
            allSemesterCourses = state.allSemesterCourses,
            onDismiss = { smartIngestResult = null },
            onConfirmShiftOverride = { override ->
                viewModel.saveShiftOverride(override)
            },
            onConfirmShiftOverrides = { overrides ->
                viewModel.saveShiftOverrides(overrides)
            },
            onConfirmAffairs = { affairs ->
                viewModel.saveAffairs(affairs)
            },
            onConfirmCourses = { courses ->
                viewModel.saveCourses(courses)
            },
            onConfirmCalendar = { semester, holidays ->
                viewModel.saveCalendar(semester, holidays)
            },
            onConfirmHolidays = { holidays ->
                viewModel.saveHolidays(holidays)
            },
            onConfirmPeriodSlots = { slots ->
                viewModel.savePeriodSlots(slots)
            }
        )
    }
}

@Composable
fun TodayHeader(
    weekNumber: Int,
    phase: String,
    dateString: String,
    isHoliday: Boolean,
    holidayName: String?,
    hasShiftOverride: Boolean = false,
    onOpenShiftDialog: () -> Unit = {},
    onOpenSmartIngest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val displayPhase = when (phase) {
        "教学周" -> stringResource(R.string.phase_teaching)
        "考试周" -> stringResource(R.string.phase_exam)
        "实践周" -> stringResource(R.string.phase_practice)
        "假期/非教学周" -> stringResource(R.string.phase_break)
        else -> phase
    }
    var menuExpanded by remember { mutableStateOf(false) }
    val accentColor = LocalAccentColor.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateString,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${stringResource(R.string.week_format, weekNumber)} · $displayPhase",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (hasShiftOverride) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentOrange.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "已调课",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (isHoliday && !holidayName.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFD8D6))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.holiday_tag_format, holidayName),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFBA1A1A),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Discreet 2nd-level menu for Shift Course & Smart Ingestion
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多设置",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI 智能全能录入", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenSmartIngest()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = AccentOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("今日调课设置", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onOpenShiftDialog()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TodayMeetingCard(
    meeting: MeetingOccurrence,
    userRole: String = SettingsRepository.ROLE_TEACHER,
    onStartThisClass: () -> Unit
) {
    val accentColor = LocalAccentColor.current
    PureSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 16.dp,
        onClick = onStartThisClass
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Bauhaus Timetable Block
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.width(68.dp)
            ) {
                Text(
                    text = TimeUtils.formatTime(meeting.startTime),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = TimeUtils.formatTime(meeting.endTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.course_period_format, meeting.periodStart, meeting.periodEnd),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (meeting.isShiftOverride) AccentOrange else accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Middle: Course Name & Room / Teacher / Class
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = meeting.course.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (meeting.isHolidaySuppressed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (meeting.isHolidaySuppressed) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                    )
                    if (meeting.isShiftOverride) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AccentOrange.copy(alpha = 0.15f))
                                .border(0.5.dp, AccentOrange, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = meeting.shiftLabel ?: "调课",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrange
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                val subText = if (userRole == SettingsRepository.ROLE_STUDENT) {
                    val teacherOrClass = meeting.course.teacher?.takeIf { it.isNotBlank() } ?: meeting.course.className
                    if (teacherOrClass.isNotBlank()) "${meeting.course.room} · $teacherOrClass" else meeting.course.room
                } else {
                    "${meeting.course.room} · ${meeting.course.className}"
                }
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right: Tactile Quick Play Glyph button or Holiday Suppressed badge
            if (meeting.isHolidaySuppressed) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = meeting.holidayName ?: "放假停课",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (meeting.status == MeetingStatus.FINISHED) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f))
                        .clickable(onClick = onStartThisClass),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.start_class_now),
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StartClassDialog(
    courses: List<Course>,
    onDismiss: () -> Unit,
    onStart: (Course, Int) -> Unit
) {
    var selectedCourse by remember { mutableStateOf(courses.firstOrNull()) }
    var selectedDuration by remember { mutableIntStateOf(45) }

    val accentColor = LocalAccentColor.current
    val accentSubtle = LocalAccentColorSubtle.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.start_class_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.select_course_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (courses.isNotEmpty()) {
                    courses.forEach { course ->
                        val isSelected = selectedCourse?.id == course.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) accentSubtle else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) accentColor else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedCourse = course }
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = course.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${course.className} · ${course.room}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    Text(stringResource(R.string.no_course_temp), style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.select_duration_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val durationOptions = listOf(
                        2 to stringResource(R.string.duration_test),
                        45 to stringResource(R.string.duration_single),
                        90 to stringResource(R.string.duration_double),
                        180 to stringResource(R.string.duration_quad)
                    )
                    durationOptions.forEach { (mins, label) ->
                        FilterChip(
                            selected = selectedDuration == mins,
                            onClick = { selectedDuration = mins },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val courseToStart = selectedCourse ?: Course(
                        id = 9999L,
                        semesterId = 1L,
                        name = "Class Meeting",
                        dayOfWeek = 1,
                        periodStart = 1,
                        periodEnd = 2,
                        weekFrom = 1,
                        weekTo = 18,
                        room = "Room 101",
                        className = "Batch 01",
                        studentCount = 45
                    )
                    onStart(courseToStart, selectedDuration)
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text(stringResource(R.string.start_class_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
