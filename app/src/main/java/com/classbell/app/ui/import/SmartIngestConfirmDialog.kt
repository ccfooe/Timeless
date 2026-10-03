package com.classbell.app.ui.import

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.classbell.app.R
import com.classbell.app.core.TimeUtils
import com.classbell.app.data.model.Affair
import com.classbell.app.data.model.AffairCategory
import com.classbell.app.data.model.Course
import com.classbell.app.data.model.Holiday
import com.classbell.app.data.model.HolidayType
import com.classbell.app.data.model.Override
import com.classbell.app.data.model.OverrideKind
import com.classbell.app.data.model.PeriodSlot
import com.classbell.app.data.model.Semester
import com.classbell.app.data.model.WeekPattern
import com.classbell.app.llm.ParsedAffair
import com.classbell.app.llm.UniversalIngestResult
import com.classbell.app.ui.components.PureSurface
import com.classbell.app.ui.theme.AccentAmber
import com.classbell.app.ui.theme.AccentOrange
import com.classbell.app.ui.theme.LocalAccentColor
import com.classbell.app.ui.theme.SystemGreen
import java.time.LocalDate
import java.time.LocalTime

data class EditableAffairItem(
    var isIncluded: Boolean = true,
    var title: String,
    var date: LocalDate,
    var time: LocalTime?,
    var place: String?,
    var category: String,
    var notes: String?
)

data class EditableShiftItem(
    val isIncluded: Boolean = true,
    val targetDate: LocalDate,
    val kind: OverrideKind,
    val followsWeekday: Int,
    val courseName: String? = null,
    val targetPeriodStart: Int? = null,
    val targetPeriodEnd: Int? = null,
    val targetRoom: String? = null,
    val note: String = ""
)

@Composable
fun SmartIngestConfirmDialog(
    result: UniversalIngestResult,
    activeSemesterId: Long = 1L,
    allSemesterCourses: List<Course> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmShiftOverride: ((Override) -> Unit)? = null,
    onConfirmShiftOverrides: ((List<Override>) -> Unit)? = null,
    onConfirmAffairs: ((List<Affair>) -> Unit)? = null,
    onConfirmCourses: ((List<Course>) -> Unit)? = null,
    onConfirmCalendar: ((Semester, List<Holiday>) -> Unit)? = null,
    onConfirmHolidays: ((List<Holiday>) -> Unit)? = null,
    onConfirmPeriodSlots: ((List<PeriodSlot>) -> Unit)? = null
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current

    // 1. Shift Overrides Editable State (Multi-shift support)
    val rawShifts = result.resolvedShiftOverrides()
    val editableShifts = remember {
        mutableStateListOf<EditableShiftItem>().apply {
            rawShifts.forEach { s ->
                val pDate = runCatching { LocalDate.parse(s.targetDate) }.getOrDefault(LocalDate.now())
                val pKind = when (s.kind.uppercase()) {
                    "CANCEL" -> OverrideKind.CANCEL
                    "COURSE_ADJUST" -> OverrideKind.COURSE_ADJUST
                    else -> OverrideKind.SHIFT
                }
                add(
                    EditableShiftItem(
                        isIncluded = true,
                        targetDate = pDate,
                        kind = pKind,
                        followsWeekday = s.followsWeekday?.coerceIn(1, 7) ?: 5,
                        courseName = s.courseName,
                        targetPeriodStart = s.targetPeriodStart,
                        targetPeriodEnd = s.targetPeriodEnd,
                        targetRoom = s.targetRoom,
                        note = s.note ?: ""
                    )
                )
            }
        }
    }

    // 2. Affairs Editable State
    val editableAffairs = remember {
        mutableStateListOf<EditableAffairItem>().apply {
            result.resolvedAffairs().forEach { parsed ->
                val pDate = runCatching { LocalDate.parse(parsed.date) }.getOrDefault(LocalDate.now())
                val pTime = parsed.time?.let { t -> runCatching { LocalTime.parse(t) }.getOrNull() }
                add(
                    EditableAffairItem(
                        isIncluded = true,
                        title = parsed.title,
                        date = pDate,
                        time = pTime,
                        place = parsed.place,
                        category = parsed.category ?: "AFFAIR",
                        notes = parsed.note
                    )
                )
            }
        }
    }

    // 3. Courses Inclusion State
    val coursesToImport = remember {
        result.courses.map { c ->
            Course(
                id = 0L,
                semesterId = activeSemesterId,
                name = c.name,
                code = c.code,
                dayOfWeek = c.dayOfWeek.coerceIn(1, 7),
                periodStart = c.periodStart.coerceIn(1, 12),
                periodEnd = c.periodEnd.coerceIn(1, 12),
                weekFrom = c.weeks?.from ?: 1,
                weekTo = c.weeks?.to ?: 18,
                weekPattern = when (c.weeks?.pattern?.lowercase()) {
                    "odd" -> WeekPattern.ODD
                    "even" -> WeekPattern.EVEN
                    else -> WeekPattern.ALL
                },
                room = c.room ?: "",
                className = c.className ?: "",
                studentCount = c.studentCount ?: 0,
                teacher = c.teacher,
                nature = c.nature,
                examType = c.examType
            )
        }
    }
    val courseInclusion = remember {
        mutableStateListOf<Boolean>().apply {
            coursesToImport.forEach { _ -> add(true) }
        }
    }

    fun handleConfirmSave() {
        // Save Shift Overrides if active
        val shiftsToSave = editableShifts.filter { it.isIncluded }.map {
            Override(
                id = 0L,
                semesterId = activeSemesterId,
                date = it.targetDate,
                kind = it.kind,
                followsWeekday = if (it.kind == OverrideKind.SHIFT) it.followsWeekday else null,
                note = it.note.ifBlank { null }
            )
        }
        if (shiftsToSave.isNotEmpty()) {
            if (onConfirmShiftOverrides != null) {
                onConfirmShiftOverrides(shiftsToSave)
            } else if (onConfirmShiftOverride != null) {
                shiftsToSave.forEach { onConfirmShiftOverride(it) }
            }
        }

        // Save Selected Affairs
        if (editableAffairs.any { it.isIncluded } && onConfirmAffairs != null) {
            val affairsToSave = editableAffairs.filter { it.isIncluded }.map {
                val cat = when (it.category.uppercase()) {
                    "MEETING" -> AffairCategory.MEETING
                    "EXAM" -> AffairCategory.EXAM
                    "OTHER" -> AffairCategory.OTHER
                    else -> AffairCategory.AFFAIR
                }
                Affair(
                    id = 0L,
                    title = it.title.ifBlank { "事项待办" },
                    date = it.date,
                    time = it.time,
                    place = it.place?.ifBlank { null },
                    category = cat,
                    note = it.notes?.ifBlank { null },
                    isDone = false
                )
            }
            onConfirmAffairs(affairsToSave)
        }

        // Save Selected Courses
        if (coursesToImport.isNotEmpty() && onConfirmCourses != null) {
            val selectedCourses = coursesToImport.filterIndexed { index, _ -> courseInclusion.getOrElse(index) { true } }
            if (selectedCourses.isNotEmpty()) {
                onConfirmCourses(selectedCourses)
            }
        }

        // Save Calendar if present
        result.calendar?.let { cal ->
            val holidays = cal.holidays.map {
                Holiday(
                    id = 0L,
                    semesterId = activeSemesterId,
                    name = it.name,
                    startDate = runCatching { LocalDate.parse(it.startDate) }.getOrDefault(LocalDate.now()),
                    endDate = runCatching { LocalDate.parse(it.endDate) }.getOrDefault(LocalDate.now()),
                    type = if (it.type == "makeup") HolidayType.MAKEUP else HolidayType.REST
                )
            }
            if (cal.semesterName.isNotBlank() && onConfirmCalendar != null) {
                val week1Mon = runCatching { LocalDate.parse(cal.week1Monday) }.getOrDefault(LocalDate.of(2026, 8, 24))
                val semester = Semester(
                    id = 0L,
                    name = cal.semesterName,
                    week1Monday = week1Mon,
                    teachingFrom = cal.teachingWeeks.from,
                    teachingTo = cal.teachingWeeks.to,
                    examFrom = cal.examWeeks?.from,
                    examTo = cal.examWeeks?.to,
                    practiceFrom = cal.practiceWeeks?.from,
                    practiceTo = cal.practiceWeeks?.to,
                    notes = cal.notes.joinToString("; "),
                    isActive = true
                )
                onConfirmCalendar(semester, holidays)
            } else if (onConfirmHolidays != null && holidays.isNotEmpty()) {
                onConfirmHolidays(holidays)
            }
        }

        // Save Period Slots if present
        if (result.periodSlots.isNotEmpty() && onConfirmPeriodSlots != null) {
            val slots = result.periodSlots.map {
                PeriodSlot(
                    id = 0L,
                    order = it.order,
                    label = it.label ?: "第${it.order}节",
                    startTime = runCatching { LocalTime.parse(it.startTime) }.getOrDefault(LocalTime.of(8, 30)),
                    endTime = runCatching { LocalTime.parse(it.endTime) }.getOrDefault(LocalTime.of(9, 15))
                )
            }
            onConfirmPeriodSlots(slots)
        }

        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val intentColor = when (result.primaryIntent.uppercase()) {
                        "SHIFT_OVERRIDE" -> AccentOrange
                        "AFFAIR" -> SystemGreen
                        "COURSE_TABLE" -> Color(0xFF0077B6)
                        else -> accentColor
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(intentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (result.primaryIntent.uppercase()) {
                                "SHIFT_OVERRIDE" -> Icons.Default.SwapHoriz
                                "AFFAIR" -> Icons.Default.Event
                                else -> Icons.Default.CalendarMonth
                            },
                            contentDescription = null,
                            tint = intentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "识别结果核对与确认",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "请检查内容无误后点击下方录入",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "取消",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: AI Plain-language Summary Banner
                item {
                    val summaryBg = when (result.primaryIntent.uppercase()) {
                        "SHIFT_OVERRIDE" -> AccentOrange.copy(alpha = 0.12f)
                        "AFFAIR" -> SystemGreen.copy(alpha = 0.12f)
                        else -> accentColor.copy(alpha = 0.12f)
                    }
                    val summaryTextColor = when (result.primaryIntent.uppercase()) {
                        "SHIFT_OVERRIDE" -> AccentOrange
                        "AFFAIR" -> Color(0xFF059669)
                        else -> accentColor
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(summaryBg)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AI 智能解析",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = summaryTextColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = result.summary.ifBlank { "已完成智能提取，请确认：" },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Section 2: Shift Override Cards (If detected)
                if (editableShifts.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = AccentOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "调课安排 (${editableShifts.count { it.isIncluded }}/${editableShifts.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    itemsIndexed(editableShifts) { index, shift ->
                        PureSurface(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = 14.dp
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = shift.isIncluded,
                                            onCheckedChange = { editableShifts[index] = shift.copy(isIncluded = it) },
                                            colors = CheckboxDefaults.colors(checkedColor = AccentOrange)
                                        )
                                        Text(
                                            text = if (shift.kind == OverrideKind.CANCEL) "全天停课/放假" else "调课行课安排",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    IconButton(
                                        onClick = { editableShifts.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "删除",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (shift.isIncluded) {
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Date selector row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .clickable {
                                                DatePickerDialog(
                                                    context,
                                                    { _, y, m, d ->
                                                        editableShifts[index] = shift.copy(targetDate = LocalDate.of(y, m + 1, d))
                                                    },
                                                    shift.targetDate.year,
                                                    shift.targetDate.monthValue - 1,
                                                    shift.targetDate.dayOfMonth
                                                ).show()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (shift.kind == OverrideKind.CANCEL) "调离/停课日期" else "调课执行日期",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${shift.targetDate} (${TimeUtils.getWeekdayShortName(shift.targetDate.dayOfWeek.value)})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (shift.kind == OverrideKind.SHIFT) {
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Target Weekday pills
                                        Text(
                                            text = "按目标星期行课：",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            val days = listOf(1 to "周一", 2 to "周二", 3 to "周三", 4 to "周四", 5 to "周五", 6 to "周六", 7 to "周日")
                                            days.forEach { (wDay, label) ->
                                                val isSel = shift.followsWeekday == wDay
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSel) AccentOrange else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                        .clickable { editableShifts[index] = shift.copy(followsWeekday = wDay) }
                                                        .padding(vertical = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = shift.note,
                                        onValueChange = { editableShifts[index] = shift.copy(note = it) },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("调课备注说明", style = MaterialTheme.typography.labelSmall) },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: Affairs List (If detected)
                if (editableAffairs.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "日程待办事项 (${editableAffairs.count { it.isIncluded }}/${editableAffairs.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "可微调字段或勾选剔除",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    itemsIndexed(editableAffairs) { index, item ->
                        PureSurface(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = 12.dp
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isIncluded,
                                        onCheckedChange = { item.isIncluded = it; editableAffairs[index] = item.copy(isIncluded = it) },
                                        colors = CheckboxDefaults.colors(checkedColor = accentColor)
                                    )

                                    OutlinedTextField(
                                        value = item.title,
                                        onValueChange = { editableAffairs[index] = item.copy(title = it) },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("事项名称", style = MaterialTheme.typography.labelSmall) },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Date, Time & Location row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Date button
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .clickable {
                                                DatePickerDialog(
                                                    context,
                                                    { _, y, m, d -> editableAffairs[index] = item.copy(date = LocalDate.of(y, m + 1, d)) },
                                                    item.date.year,
                                                    item.date.monthValue - 1,
                                                    item.date.dayOfMonth
                                                ).show()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.date.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    // Time button
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .clickable {
                                                val cur = item.time ?: LocalTime.of(9, 0)
                                                TimePickerDialog(
                                                    context,
                                                    { _, h, min -> editableAffairs[index] = item.copy(time = LocalTime.of(h, min)) },
                                                    cur.hour,
                                                    cur.minute,
                                                    true
                                                ).show()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.time?.let { TimeUtils.formatTime(it) } ?: "全天",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                if (!item.place.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "📍 ${item.place}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 4: Courses List (If detected)
                if (coursesToImport.isNotEmpty()) {
                    item {
                        Text(
                            text = "课程表 (${coursesToImport.size}门课程)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    itemsIndexed(coursesToImport) { index, course ->
                        PureSurface(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = 12.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = courseInclusion[index],
                                    onCheckedChange = { courseInclusion[index] = it },
                                    colors = CheckboxDefaults.colors(checkedColor = accentColor)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = course.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "周${TimeUtils.getWeekdayNameChinese(course.dayOfWeek)} 第${course.periodStart}-${course.periodEnd}节 · ${course.room}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { handleConfirmSave() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("确认录入本地")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
