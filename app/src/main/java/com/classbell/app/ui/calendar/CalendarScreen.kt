package com.classbell.app.ui.calendar

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classbell.app.R
import com.classbell.app.core.TimeUtils
import com.classbell.app.core.WeekCalculator
import com.classbell.app.data.model.Affair
import com.classbell.app.data.model.AffairCategory
import com.classbell.app.data.model.Holiday
import com.classbell.app.data.model.HolidayType
import com.classbell.app.data.model.MeetingOccurrence
import com.classbell.app.data.model.Semester
import com.classbell.app.ui.theme.AccentAmber
import com.classbell.app.ui.theme.AccentOrange
import com.classbell.app.ui.theme.AccentOrangeSubtle
import com.classbell.app.ui.theme.LocalAccentColor
import com.classbell.app.ui.theme.LocalAccentColorSubtle
import com.classbell.app.ui.theme.SystemBlue
import com.classbell.app.ui.theme.SystemGreen
import com.classbell.app.ui.theme.SystemRed
import java.time.LocalDate
import java.time.YearMonth

private val WeekdayHeadersZh = listOf("周", "一", "二", "三", "四", "五", "六", "日")
private val WeekdayHeadersEn = listOf("Wk", "M", "T", "W", "T", "F", "S", "S")

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accentColor = LocalAccentColor.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddAffair() },
                containerColor = accentColor,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_affair))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ColorOS 16 Inspired Month Header with "今" (Back to Today) pill
            item {
                ColorOSMonthHeader(
                    yearMonth = state.selectedYearMonth,
                    isCurrentMonth = state.selectedYearMonth == YearMonth.now(),
                    onPrevious = { viewModel.previousMonth() },
                    onNext = { viewModel.nextMonth() },
                    onJumpToToday = {
                        val today = LocalDate.now()
                        viewModel.selectDate(today)
                    }
                )
            }

            // ColorOS 16 Calendar Grid with Week Numbers and 休/班 markers
            item {
                ColorOSCalendarGrid(
                    yearMonth = state.selectedYearMonth,
                    selectedDate = state.selectedDate,
                    semester = state.activeSemester,
                    affairsInMonth = state.allAffairsInMonth,
                    holidaysInMonth = state.holidaysInMonth,
                    onDateClick = { viewModel.selectDate(it) }
                )
            }

            // Selected Date Summary Bar
            item {
                SelectedDateBar(
                    date = state.selectedDate,
                    weekNumber = state.selectedDateWeekNumber
                )
            }

            // Timeline Layer 1: Courses on Selected Date
            if (state.selectedDateMeetings.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = AccentOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.timeline_classes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(state.selectedDateMeetings) { meeting ->
                    ColorOSMeetingCard(meeting = meeting)
                }
            }

            // Timeline Layer 2: Affairs & Tasks on Selected Date
            item {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.timeline_affairs),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (state.selectedDateAffairs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_affairs_today),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.selectedDateAffairs) { affair ->
                    ColorOSAffairCard(
                        affair = affair,
                        onToggle = { viewModel.toggleAffair(affair) },
                        onEdit = { viewModel.openEditAffair(affair) },
                        onDelete = { viewModel.deleteAffair(affair) }
                    )
                }
            }
        }

        // Add/Edit Affair Dialog
        if (state.isEditingAffair) {
            AffairEditDialog(
                initialAffair = state.affairToEdit,
                defaultDate = state.selectedDate,
                onDismiss = { viewModel.closeEditAffair() },
                onSave = { viewModel.saveAffair(it) }
            )
        }
    }
}

@Composable
fun ColorOSMonthHeader(
    yearMonth: YearMonth,
    isCurrentMonth: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onJumpToToday: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.calendar_month_format, yearMonth.year, yearMonth.monthValue),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Quick "今" (Back to Today) button
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (isCurrentMonth) AccentOrangeSubtle else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onJumpToToday),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.calendar_today_btn),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentMonth) AccentOrange else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.prev_month))
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.next_month))
            }
        }
    }
}

@Composable
fun ColorOSCalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    semester: Semester?,
    affairsInMonth: List<Affair>,
    holidaysInMonth: List<Holiday>,
    onDateClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstWeekday = firstDayOfMonth.dayOfWeek.value // 1..7

    val affairDates = affairsInMonth.map { it.date }.toSet()

    val isEn = LocalContext.current.resources.configuration.locales[0].language == "en"
    val weekdayHeaders = if (isEn) WeekdayHeadersEn else WeekdayHeadersZh

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Week | 一 | 二 | 三 | 四 | 五 | 六 | 日
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdayHeaders.forEachIndexed { index, header ->
                    Text(
                        text = header,
                        modifier = Modifier.weight(if (index == 0) 0.8f else 1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (index == 0) AccentOrange else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Days of the month
            var currentDay = 1
            var rowStartOffset = firstWeekday - 1

            while (currentDay <= daysInMonth) {
                val weekNum = if (semester != null) {
                    WeekCalculator.calculateWeekNumber(firstDayOfMonth.plusDays((currentDay - 1).toLong()), semester.week1Monday)
                } else 1

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Week Number column (W1~W21)
                    Box(
                        modifier = Modifier
                            .weight(0.8f)
                            .height(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "W$weekNum",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                    }

                    // 7 day columns
                    for (col in 0 until 7) {
                        if (rowStartOffset > 0) {
                            Box(modifier = Modifier.weight(1f))
                            rowStartOffset--
                        } else if (currentDay <= daysInMonth) {
                            val thisDate = yearMonth.atDay(currentDay)
                            val isSelected = thisDate == selectedDate
                            val isToday = thisDate == LocalDate.now()
                            val hasAffair = thisDate in affairDates

                            // Check holiday or makeup day (休 / 班)
                            val holiday = holidaysInMonth.firstOrNull {
                                !thisDate.isBefore(it.startDate) && !thisDate.isAfter(it.endDate)
                            }
                            val isRestHoliday = holiday?.type == HolidayType.REST
                            val isMakeupWorkday = holiday?.type == HolidayType.MAKEUP

                            val accentColor = LocalAccentColor.current
                            val accentSubtle = LocalAccentColorSubtle.current

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clickable { onDateClick(thisDate) },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> accentColor
                                                isToday -> accentSubtle
                                                else -> Color.Transparent
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = currentDay.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isRestHoliday) {
                                                Text(
                                                    text = stringResource(R.string.marker_holiday),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else SystemRed
                                                )
                                            } else if (isMakeupWorkday) {
                                                Text(
                                                    text = stringResource(R.string.marker_workday),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else SystemBlue
                                                )
                                            } else if (hasAffair) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(3.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) Color.White else accentColor)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            currentDay++
                        } else {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedDateBar(date: LocalDate, weekNumber: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = TimeUtils.formatDateWithWeekday(date),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AccentOrangeSubtle)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = stringResource(R.string.week_format, weekNumber),
                style = MaterialTheme.typography.labelSmall,
                color = AccentOrange,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ColorOSMeetingCard(meeting: MeetingOccurrence) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left vertical colored accent bar (ColorOS style)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AccentOrange)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meeting.course.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${meeting.course.className} · ${meeting.course.room}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${TimeUtils.formatTime(meeting.startTime)}–${TimeUtils.formatTime(meeting.endTime)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange
                )
                Text(
                    text = stringResource(R.string.course_period_format, meeting.periodStart, meeting.periodEnd),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ColorOSAffairCard(
    affair: Affair,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = when (affair.category) {
        AffairCategory.MEETING -> Color(0xFF3A86FF)
        AffairCategory.EXAM -> Color(0xFFE63946)
        AffairCategory.AFFAIR -> AccentOrange
        AffairCategory.OTHER -> Color(0xFF8338EC)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category left bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (affair.isDone) categoryColor.copy(alpha = 0.4f) else categoryColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = affair.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (affair.isDone) FontWeight.Normal else FontWeight.SemiBold,
                        color = if (affair.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (affair.isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!affair.place.isNullOrBlank()) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${affair.place} · ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        affair.time?.let {
                            Text(
                                text = TimeUtils.formatTime(it),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (affair.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else AccentOrange
                            )
                        }
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_affair), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_affair), tint = Color.Red, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
