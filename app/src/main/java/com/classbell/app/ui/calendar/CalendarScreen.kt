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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classbell.app.core.TimeUtils
import com.classbell.app.core.WeekCalculator
import com.classbell.app.data.model.Affair
import com.classbell.app.data.model.MeetingOccurrence
import com.classbell.app.data.model.Semester
import com.classbell.app.ui.theme.PinkPrimary
import java.time.LocalDate
import java.time.YearMonth

private val WeekdayHeaders = listOf("周", "一", "二", "三", "四", "五", "六", "日")

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddAffair() },
                containerColor = PinkPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增事务")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Header Selector
            item {
                MonthHeader(
                    yearMonth = state.selectedYearMonth,
                    onPrevious = { viewModel.previousMonth() },
                    onNext = { viewModel.nextMonth() }
                )
            }

            // Month Grid with Row Week Numbers
            item {
                MonthCalendarGrid(
                    yearMonth = state.selectedYearMonth,
                    selectedDate = state.selectedDate,
                    semester = state.activeSemester,
                    affairsInMonth = state.allAffairsInMonth,
                    onDateClick = { viewModel.selectDate(it) }
                )
            }

            // Selected Date Summary Header
            item {
                SelectedDateHeader(
                    date = state.selectedDate,
                    weekNumber = state.selectedDateWeekNumber
                )
            }

            // Layer 1 & 2: Classes on Selected Date
            if (state.selectedDateMeetings.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = PinkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "课程安排",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(state.selectedDateMeetings) { meeting ->
                    CalendarMeetingItem(meeting = meeting)
                }
            }

            // Layer 3: Affairs on Selected Date
            item {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = PinkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "校务与日程",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (state.selectedDateAffairs.isEmpty()) {
                item {
                    Text(
                        text = "暂无日程待办",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(state.selectedDateAffairs) { affair ->
                    CalendarAffairItem(
                        affair = affair,
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
fun MonthHeader(
    yearMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
        }

        Text(
            text = "${yearMonth.year} 年 ${yearMonth.monthValue} 月",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
        }
    }
}

@Composable
fun MonthCalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    semester: Semester?,
    affairsInMonth: List<Affair>,
    onDateClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstWeekday = firstDayOfMonth.dayOfWeek.value // 1..7

    val affairDates = affairsInMonth.map { it.date }.toSet()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row: Week | Mon | Tue | Wed | Thu | Fri | Sat | Sun
            Row(modifier = Modifier.fillMaxWidth()) {
                WeekdayHeaders.forEachIndexed { index, header ->
                    Text(
                        text = header,
                        modifier = Modifier.weight(if (index == 0) 0.8f else 1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (index == 0) PinkPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar weeks
            var currentDay = 1
            var rowStartOffset = firstWeekday - 1

            while (currentDay <= daysInMonth) {
                val rowMondayDate = firstDayOfMonth.plusDays((currentDay - rowStartOffset - 1).coerceAtLeast(0).toLong())
                val weekNum = if (semester != null) {
                    WeekCalculator.calculateWeekNumber(firstDayOfMonth.plusDays((currentDay - 1).toLong()), semester.week1Monday)
                } else 1

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Week Number Label
                    Text(
                        text = "W$weekNum",
                        modifier = Modifier.weight(0.8f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PinkPrimary.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    // 7 days of this week row
                    for (col in 0 until 7) {
                        if (rowStartOffset > 0) {
                            // Blank pre-month cell
                            Box(modifier = Modifier.weight(1f))
                            rowStartOffset--
                        } else if (currentDay <= daysInMonth) {
                            val thisDate = yearMonth.atDay(currentDay)
                            val isSelected = thisDate == selectedDate
                            val isToday = thisDate == LocalDate.now()
                            val hasAffair = thisDate in affairDates

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> PinkPrimary
                                            isToday -> PinkPrimary.copy(alpha = 0.15f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onDateClick(thisDate) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = currentDay.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (hasAffair) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color.White else PinkPrimary)
                                        )
                                    }
                                }
                            }
                            currentDay++
                        } else {
                            // Blank post-month cell
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedDateHeader(date: LocalDate, weekNumber: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = TimeUtils.formatDateWithWeekday(date),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(PinkPrimary.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "第 $weekNumber 周",
                style = MaterialTheme.typography.labelSmall,
                color = PinkPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CalendarMeetingItem(meeting: MeetingOccurrence) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = meeting.course.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${meeting.course.className} · ${meeting.course.room}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${TimeUtils.formatTime(meeting.startTime)}–${TimeUtils.formatTime(meeting.endTime)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = PinkPrimary
            )
        }
    }
}

@Composable
fun CalendarAffairItem(
    affair: Affair,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = affair.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!affair.place.isNullOrBlank()) {
                    Text(
                        text = "地点: ${affair.place}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                affair.time?.let {
                    Text(
                        text = "时间: ${TimeUtils.formatTime(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PinkPrimary
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.Red, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
