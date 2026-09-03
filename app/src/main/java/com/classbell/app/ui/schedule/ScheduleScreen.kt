package com.classbell.app.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classbell.app.core.TimeUtils
import com.classbell.app.data.model.Course
import com.classbell.app.data.model.MeetingOccurrence
import com.classbell.app.data.model.PeriodSlot
import com.classbell.app.ui.glass.GlassCard
import com.classbell.app.ui.theme.PinkPrimary

private val WeekdayNames = listOf("一", "二", "三", "四", "五", "六", "日")
private val SlotRowHeight = 64.dp
private val TimeColWidth = 44.dp
private val DayColWidth = 82.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddCourse() },
                containerColor = PinkPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增课程")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Week Switcher Top Bar
            WeekSwitcherBar(
                selectedWeek = state.selectedWeek,
                currentWeek = state.currentWeekNumber,
                dateRange = state.weekDateRangeStr,
                phaseLabel = state.phaseLabel,
                onPrevious = { viewModel.previousWeek() },
                onNext = { viewModel.nextWeek() },
                onResetCurrent = { viewModel.selectWeek(state.currentWeekNumber) }
            )

            // Outside Teaching Week Banner (e.g. 考试周 / 实践周)
            if (!state.isTeachingWeek) {
                PhaseBanner(phaseLabel = state.phaseLabel, weekNumber = state.selectedWeek)
            } else {
                // 10-period dynamic grid
                ScheduleGrid(
                    periodSlots = state.periodSlots,
                    weeklyMeetings = state.weeklyMeetings,
                    onMeetingClick = { viewModel.openCourseDetail(it) }
                )
            }
        }

        // Course Detail Bottom Sheet
        if (state.selectedMeetingForDetail != null) {
            val meeting = state.selectedMeetingForDetail!!
            ModalBottomSheet(
                onDismissRequest = { viewModel.closeCourseDetail() },
                sheetState = sheetState
            ) {
                CourseDetailSheetContent(
                    meeting = meeting,
                    onEdit = { viewModel.openEditCourse(meeting.course) },
                    onDelete = { viewModel.deleteCourse(meeting.course) }
                )
            }
        }

        // Course Add / Edit Dialog
        if (state.isEditingCourse) {
            CourseEditDialog(
                initialCourse = state.courseToEdit,
                semesterId = state.activeSemester?.id ?: 1L,
                onDismiss = { viewModel.closeEditCourse() },
                onSave = { viewModel.saveCourse(it) }
            )
        }
    }
}

@Composable
fun WeekSwitcherBar(
    selectedWeek: Int,
    currentWeek: Int,
    dateRange: String,
    phaseLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onResetCurrent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "上一周")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "第 $selectedWeek 周",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PinkPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = phaseLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = PinkPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (dateRange.isNotBlank()) {
                    Text(
                        text = dateRange,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onNext) {
                Icon(Icons.Default.ChevronRight, contentDescription = "下一周")
            }
        }

        if (selectedWeek != currentWeek) {
            TextButton(onClick = onResetCurrent) {
                Text("本周", color = PinkPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PhaseBanner(phaseLabel: String, weekNumber: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🗓️",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "第 $weekNumber 周 · $phaseLabel",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "本周不进行常规理论课程教学，安排集中 $phaseLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ScheduleGrid(
    periodSlots: List<PeriodSlot>,
    weeklyMeetings: Map<Int, List<MeetingOccurrence>>,
    onMeetingClick: (MeetingOccurrence) -> Unit,
    modifier: Modifier = Modifier
) {
    val hScrollState = rememberScrollState()
    val vScrollState = rememberScrollState()

    val slots = if (periodSlots.isNotEmpty()) periodSlots else (1..10).map {
        PeriodSlot(it.toLong(), it, "第${it}节", java.time.LocalTime.of(8, 30), java.time.LocalTime.of(9, 15))
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Weekday Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = TimeColWidth)
                .horizontalScroll(hScrollState)
        ) {
            (1..7).forEach { day ->
                Box(
                    modifier = Modifier
                        .width(DayColWidth)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "周${WeekdayNames[day - 1]}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Grid Body (Periods + Days)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(vScrollState)
        ) {
            // Period Column (Fixed left)
            Column(modifier = Modifier.width(TimeColWidth)) {
                slots.forEach { slot ->
                    Box(
                        modifier = Modifier
                            .height(SlotRowHeight)
                            .fillMaxWidth()
                            .border(0.5.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = slot.order.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${slot.startTime.hour}:${slot.startTime.minute.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 7 Days Columns (Scrollable horizontally)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(hScrollState)
            ) {
                (1..7).forEach { day ->
                    val meetingsForDay = weeklyMeetings[day] ?: emptyList()

                    Box(
                        modifier = Modifier
                            .width(DayColWidth)
                            .height(SlotRowHeight * slots.size)
                            .border(0.5.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        // Empty slot lines
                        Column {
                            slots.forEach { _ ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(SlotRowHeight)
                                        .border(0.5.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                )
                            }
                        }

                        // Render Course blocks over slots
                        meetingsForDay.forEach { meeting ->
                            val startOrder = meeting.periodStart.coerceIn(1, slots.size)
                            val endOrder = meeting.periodEnd.coerceIn(startOrder, slots.size)
                            val span = endOrder - startOrder + 1

                            val topOffset = SlotRowHeight * (startOrder - 1)
                            val blockHeight = SlotRowHeight * span - 4.dp

                            val blockColor = runCatching {
                                Color(android.graphics.Color.parseColor(meeting.course.color))
                            }.getOrDefault(PinkPrimary)

                            Box(
                                modifier = Modifier
                                    .padding(top = topOffset, start = 2.dp, end = 2.dp)
                                    .fillMaxWidth()
                                    .height(blockHeight)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(blockColor.copy(alpha = 0.88f))
                                    .clickable { onMeetingClick(meeting) }
                                    .padding(6.dp)
                            ) {
                                Column {
                                    Text(
                                        text = meeting.course.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = meeting.course.room,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                    Text(
                                        text = meeting.course.className,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = Color.White.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CourseDetailSheetContent(
    meeting: MeetingOccurrence,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = meeting.course.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.Red)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        DetailRow(label = "授课班级", value = meeting.course.className)
        DetailRow(label = "上课人数", value = "${meeting.course.studentCount} 人")
        DetailRow(label = "节次时间", value = "第 ${meeting.periodStart}–${meeting.periodEnd} 节 (${TimeUtils.formatTime(meeting.startTime)}–${TimeUtils.formatTime(meeting.endTime)})")
        DetailRow(label = "上课地点", value = "${meeting.course.campus ?: ""} · ${meeting.course.room}")
        DetailRow(label = "周次安排", value = "${meeting.course.weekFrom}–${meeting.course.weekTo} 周 (${when (meeting.course.weekPattern) {
            com.classbell.app.data.model.WeekPattern.ALL -> "每周"
            com.classbell.app.data.model.WeekPattern.ODD -> "单周"
            com.classbell.app.data.model.WeekPattern.EVEN -> "双周"
        }})")
        meeting.course.nature?.let { DetailRow(label = "课程性质", value = it) }
        meeting.course.examType?.let { DetailRow(label = "考核方式", value = it) }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
