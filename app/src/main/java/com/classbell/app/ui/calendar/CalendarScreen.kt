package com.classbell.app.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import com.classbell.app.data.model.Course
import com.classbell.app.data.model.Override
import com.classbell.app.data.model.OverrideKind
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import com.classbell.app.ui.theme.AccentOrange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
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
    val context = LocalContext.current

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

            // ColorOS 16 Calendar Grid with Week Numbers and 休/班/调 markers
            item {
                ColorOSCalendarGrid(
                    yearMonth = state.selectedYearMonth,
                    selectedDate = state.selectedDate,
                    semester = state.activeSemester,
                    affairsInMonth = state.allAffairsInMonth,
                    holidaysInMonth = state.holidaysInMonth,
                    overridesInSemester = state.overridesInSemester,
                    holidaysInSemester = state.holidaysInSemester,
                    autoTimezone = state.autoTimezoneConversion,
                    onDateClick = { viewModel.selectDate(it) }
                )
            }

            // Selected Date Summary Bar
            item {
                SelectedDateBar(
                    date = state.selectedDate,
                    weekNumber = state.selectedDateWeekNumber,
                    hasOverride = state.overridesInSemester.any { it.date == state.selectedDate },
                    onOpenShiftDialog = { viewModel.openShiftDialog() }
                )
            }

            // Prominent Shift Override Status Card (Allows direct instant cancellation or editing)
            val selectedDateOverrides = state.overridesInSemester.filter { it.date == state.selectedDate }
            if (selectedDateOverrides.isNotEmpty()) {
                item {
                    ShiftOverrideStatusCard(
                        date = state.selectedDate,
                        overrides = selectedDateOverrides,
                        courses = state.allCoursesInSemester,
                        onClear = {
                            viewModel.clearOverridesForDate(state.selectedDate)
                            android.widget.Toast.makeText(context, "已取消该日调课状态，恢复正常课表", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onEdit = {
                            viewModel.openShiftDialog()
                        }
                    )
                }
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
                            tint = accentColor,
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
                        autoTimezone = state.autoTimezoneConversion,
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
                onSave = { viewModel.saveAffair(it) },
                onSmartParse = { text -> viewModel.parseAffair(text) }
            )
        }

        // Shift Course Dialog
        if (state.isEditingShift) {
            val dateOverrides = state.overridesInSemester.filter { it.date == state.selectedDate }
            ShiftCourseDialog(
                targetDate = state.selectedDate,
                courses = state.allCoursesInSemester,
                currentOverrides = dateOverrides,
                onSaveWholeDayShift = { date, follows, note -> viewModel.saveWholeDayShift(date, follows, note) },
                onSaveWholeDayCancel = { date, note -> viewModel.saveWholeDayCancel(date, note) },
                onSaveSingleAdjustment = { date, cId, pStart, pEnd, room, note -> viewModel.saveSingleAdjustment(date, cId, pStart, pEnd, room, note) },
                onSaveSingleCancel = { date, cId, note -> viewModel.saveSingleCancel(date, cId, note) },
                onClearOverrides = { date -> viewModel.clearOverridesForDate(date) },
                onDismiss = { viewModel.closeShiftDialog() }
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
    val accentColor = LocalAccentColor.current

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
            Spacer(modifier = Modifier.width(10.dp))

            // Quick "今" (Back to Today) button (OnePlus/Apple style high-contrast pill/capsule)
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onJumpToToday),
                shape = RoundedCornerShape(14.dp),
                color = if (isCurrentMonth) accentColor.copy(alpha = 0.14f) else accentColor,
                border = if (isCurrentMonth) BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)) else null
            ) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 28.dp, minHeight = 28.dp)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.calendar_today_btn),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrentMonth) accentColor else Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
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
    overridesInSemester: List<Override> = emptyList(),
    holidaysInSemester: List<Holiday> = emptyList(),
    autoTimezone: Boolean = false,
    onDateClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstWeekday = firstDayOfMonth.dayOfWeek.value // 1..7

    val affairDates = affairsInMonth.map {
        com.classbell.app.core.TimeUtils.convertAffairDateTime(it.date, it.time, autoTimezone).effectiveDate
    }.toSet()

    val isEn = java.util.Locale.getDefault().language == "en"
    val weekdayHeaders = if (isEn) WeekdayHeadersEn else WeekdayHeadersZh
    val accentColor = LocalAccentColor.current

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
                        color = if (index == 0) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
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
                            color = accentColor.copy(alpha = 0.85f),
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

                            val dayOverrides = overridesInSemester.filter { it.date == thisDate }
                            val isShiftOverride = dayOverrides.any { it.kind == OverrideKind.SHIFT || it.kind == OverrideKind.COURSE_ADJUST }
                            val isCancelOverride = dayOverrides.any { it.kind == OverrideKind.CANCEL && it.courseId == null }

                            // Check holiday or makeup day (休 / 班)
                            val holiday = (holidaysInMonth.ifEmpty { holidaysInSemester }).firstOrNull {
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
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onDateClick(thisDate) },
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

                                        if (hasAffair) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 1.dp)
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color.White else accentColor)
                                            )
                                        }
                                    }
                                }

                                // Top-Right Badge: 调 (orange) / 班 (blue) / 休 (red)
                                if (isShiftOverride) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(end = 2.dp, top = 2.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(AccentOrange)
                                            .padding(horizontal = 2.5.dp, vertical = 0.5.dp)
                                    ) {
                                        Text("调", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                } else if (isMakeupWorkday) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(end = 2.dp, top = 2.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(SystemBlue)
                                            .padding(horizontal = 2.5.dp, vertical = 0.5.dp)
                                    ) {
                                        Text("班", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                } else if (isRestHoliday || isCancelOverride) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(end = 2.dp, top = 2.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(SystemRed)
                                            .padding(horizontal = 2.5.dp, vertical = 0.5.dp)
                                    ) {
                                        Text("休", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
fun SelectedDateBar(
    date: LocalDate,
    weekNumber: Int,
    hasOverride: Boolean = false,
    onOpenShiftDialog: () -> Unit
) {
    val accentColor = LocalAccentColor.current
    val accentColorSubtle = LocalAccentColorSubtle.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = TimeUtils.formatDateWithWeekday(date),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColorSubtle)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = stringResource(R.string.week_format, weekNumber),
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Minimalist "调课设置" action pill
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenShiftDialog),
            shape = RoundedCornerShape(12.dp),
            color = if (hasOverride) AccentOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = if (hasOverride) BorderStroke(1.dp, AccentOrange.copy(alpha = 0.4f)) else null
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = if (hasOverride) AccentOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (hasOverride) "已调课" else "调课设置",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (hasOverride) AccentOrange else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ShiftOverrideStatusCard(
    date: LocalDate,
    overrides: List<Override>,
    courses: List<Course>,
    onClear: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AccentOrange.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, AccentOrange.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(AccentOrange.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = AccentOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "今日已设置调课规则",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentOrange)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "调课生效中",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            overrides.forEach { ov ->
                val desc = when (ov.kind) {
                    OverrideKind.SHIFT -> {
                        val dayName = when (ov.followsWeekday) {
                            1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"; 5 -> "周五"; 6 -> "周六"; else -> "周日"
                        }
                        "全天执行【$dayName】课表" + (ov.note?.let { " · $it" } ?: "")
                    }
                    OverrideKind.CANCEL -> {
                        if (ov.courseId == null) "全天停课/放假" + (ov.note?.let { " · $it" } ?: "")
                        else {
                            val cName = courses.firstOrNull { it.id == ov.courseId }?.name ?: "课程"
                            "【$cName】单课停课" + (ov.note?.let { " · $it" } ?: "")
                        }
                    }
                    OverrideKind.COURSE_ADJUST -> {
                        val cName = courses.firstOrNull { it.id == ov.courseId }?.name ?: "课程"
                        "【$cName】改至第${ov.targetPeriodStart}-${ov.targetPeriodEnd}节 · ${ov.targetRoom ?: ""}" + (ov.note?.let { " · $it" } ?: "")
                    }
                }
                Text(
                    text = "• $desc",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Clear button (instantly clears and restores normal calendar)
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("取消调课状态", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Edit button (opens dialog)
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AccentOrange.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = AccentOrange
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("修改调课设置", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ColorOSMeetingCard(meeting: MeetingOccurrence) {
    val accentColor = LocalAccentColor.current

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
                    .background(
                        when {
                            meeting.isHolidaySuppressed -> Color.Gray.copy(alpha = 0.35f)
                            meeting.isShiftOverride -> AccentOrange
                            else -> accentColor
                        }
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = meeting.course.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (meeting.isHolidaySuppressed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (meeting.isHolidaySuppressed) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                    )
                    if (meeting.isHolidaySuppressed) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = meeting.holidayName ?: "放假停课",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (meeting.isShiftOverride) {
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
                    color = when {
                        meeting.isHolidaySuppressed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        meeting.isShiftOverride -> AccentOrange
                        else -> accentColor
                    },
                    textDecoration = if (meeting.isHolidaySuppressed) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
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
    autoTimezone: Boolean = false,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = LocalAccentColor.current
    val categoryColor = when (affair.category) {
        AffairCategory.MEETING -> Color(0xFF3A86FF)
        AffairCategory.EXAM -> Color(0xFFE63946)
        AffairCategory.AFFAIR -> accentColor
        AffairCategory.OTHER -> Color(0xFF8338EC)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Dedicated Round Checkbox
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(28.dp)
            ) {
                if (affair.isDone) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.affair_status_done),
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(
                                width = 1.8.dp,
                                color = if (affair.isDone) Color.Gray.copy(alpha = 0.5f) else categoryColor.copy(alpha = 0.85f),
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 2. Category vertical bar
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (affair.isDone) categoryColor.copy(alpha = 0.35f) else categoryColor)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 3. Middle Content: Title + Place
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = affair.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (affair.isDone) FontWeight.Normal else FontWeight.SemiBold,
                    color = if (affair.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (affair.isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!affair.place.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = affair.place,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 4. Right: Time Badge on top, Edit & Delete buttons below
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val conv = com.classbell.app.core.TimeUtils.convertAffairDateTime(affair.date, affair.time, autoTimezone)
                conv.effectiveTime?.let { effTime ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (affair.isDone) Color.Gray.copy(alpha = 0.12f) else categoryColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = TimeUtils.formatTime(effTime),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (affair.isDone) Color.Gray else categoryColor,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (conv.isConverted) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = stringResource(R.string.timezone_badge_local_short),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_affair),
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete_affair),
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
