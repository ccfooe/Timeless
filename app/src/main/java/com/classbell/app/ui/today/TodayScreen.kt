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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import com.classbell.app.data.model.Course
import com.classbell.app.data.model.MeetingOccurrence
import com.classbell.app.data.model.MeetingStatus
import com.classbell.app.ui.calendar.AffairEditDialog
import com.classbell.app.ui.components.PureSurface
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
            // 1. Header: Date + Week + Phase Badge
            item {
                TodayHeader(
                    weekNumber = state.weekNumber,
                    phase = state.semesterPhase,
                    dateString = TimeUtils.formatHeaderDate(state.currentDate),
                    isHoliday = state.isHolidayToday,
                    holidayName = state.holidayName
                )
            }

            // 2. Live Activity / Now-In-Class Active Card & Today's Tasks
            item {
                NowInClassCard(
                    currentMeeting = state.currentMeeting,
                    nextMeeting = state.nextMeeting,
                    currentTime = state.currentTime,
                    todayAffairs = state.todayAffairs,
                    onToggleAffair = { viewModel.toggleAffair(it) },
                    onAddAffair = { showAddAffairDialog = true },
                    onEndClass = { viewModel.endClassNow() },
                    onExtendClass = { viewModel.extendClass(it) }
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
                                    text = stringResource(R.string.hero_start_class_title),
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.today_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                        onStartThisClass = {
                            viewModel.startCourseRoutine(meeting)
                        }
                    )
                }
            }
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
    modifier: Modifier = Modifier
) {
    val displayPhase = when (phase) {
        "教学周" -> stringResource(R.string.phase_teaching)
        "考试周" -> stringResource(R.string.phase_exam)
        "实践周" -> stringResource(R.string.phase_practice)
        "假期/非教学周" -> stringResource(R.string.phase_break)
        else -> phase
    }

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

            if (isHoliday && !holidayName.isNullOrBlank()) {
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
        }
    }
}

@Composable
fun TodayMeetingCard(
    meeting: MeetingOccurrence,
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
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Middle: Course Name & Room
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meeting.course.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${meeting.course.room} · ${meeting.course.className}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right: Tactile Quick Play Glyph button
            if (meeting.status == MeetingStatus.FINISHED) {
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
