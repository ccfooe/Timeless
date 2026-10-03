package com.classbell.app.ui.schedule

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classbell.app.R
import com.classbell.app.core.TimeUtils
import com.classbell.app.data.model.Course
import com.classbell.app.data.model.WeekPattern
import com.classbell.app.ui.theme.AccentOrange
import com.classbell.app.ui.theme.CourseColors
import com.classbell.app.ui.theme.PinkPrimary

@Composable
fun CourseEditDialog(
    initialCourse: Course?,
    semesterId: Long,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit
) {
    var name by remember { mutableStateOf(initialCourse?.name ?: "") }
    var code by remember { mutableStateOf(initialCourse?.code ?: "") }
    var dayOfWeek by remember { mutableIntStateOf(initialCourse?.dayOfWeek ?: 3) }
    var periodStart by remember { mutableIntStateOf(initialCourse?.periodStart ?: 1) }
    var periodEnd by remember { mutableIntStateOf(initialCourse?.periodEnd ?: 2) }
    var weekFrom by remember { mutableIntStateOf(initialCourse?.weekFrom ?: 1) }
    var weekTo by remember { mutableIntStateOf(initialCourse?.weekTo ?: 16) }
    var weekPattern by remember { mutableStateOf(initialCourse?.weekPattern ?: WeekPattern.ALL) }
    var campus by remember { mutableStateOf(initialCourse?.campus ?: "") }
    var room by remember { mutableStateOf(initialCourse?.room ?: "") }
    var className by remember { mutableStateOf(initialCourse?.className ?: "") }
    var teacher by remember { mutableStateOf(initialCourse?.teacher ?: "") }
    var studentCountStr by remember { mutableStateOf(initialCourse?.studentCount?.toString() ?: "45") }
    var nature by remember { mutableStateOf(initialCourse?.nature ?: "必修") }
    var examType by remember { mutableStateOf(initialCourse?.examType ?: "考试") }
    var selectedColor by remember { mutableStateOf(initialCourse?.color ?: CourseColors[0]) }

    val isEn = java.util.Locale.getDefault().language == "en"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCourse == null) stringResource(R.string.add_course) else stringResource(R.string.edit_course),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.course_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text(stringResource(R.string.course_teacher_label)) },
                    placeholder = { Text(stringResource(R.string.course_teacher_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = className,
                        onValueChange = { className = it },
                        label = { Text(stringResource(R.string.course_class_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )

                    OutlinedTextField(
                        value = studentCountStr,
                        onValueChange = { studentCountStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(R.string.course_students_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = campus,
                        onValueChange = { campus = it },
                        label = { Text(stringResource(R.string.course_campus_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text(stringResource(R.string.course_room_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Day of Week Selector
                val dayName = if (isEn) listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[dayOfWeek - 1] else TimeUtils.getWeekdayShortName(dayOfWeek)
                Text(stringResource(R.string.course_weekday_label, dayName), style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (1..7).forEach { day ->
                        FilterChip(
                            selected = dayOfWeek == day,
                            onClick = { dayOfWeek = day },
                            label = { Text(day.toString()) }
                        )
                    }
                }

                // Period Range
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = periodStart.toString(),
                        onValueChange = { periodStart = it.toIntOrNull()?.coerceIn(1, 10) ?: periodStart },
                        label = { Text(stringResource(R.string.course_period_start_label)) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = periodEnd.toString(),
                        onValueChange = { periodEnd = it.toIntOrNull()?.coerceIn(1, 10) ?: periodEnd },
                        label = { Text(stringResource(R.string.course_period_end_label)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Week Range & Pattern
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = weekFrom.toString(),
                        onValueChange = { weekFrom = it.toIntOrNull()?.coerceAtLeast(1) ?: weekFrom },
                        label = { Text(stringResource(R.string.course_week_start_label)) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weekTo.toString(),
                        onValueChange = { weekTo = it.toIntOrNull()?.coerceAtLeast(1) ?: weekTo },
                        label = { Text(stringResource(R.string.course_week_end_label)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Week Pattern Choice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = weekPattern == WeekPattern.ALL,
                        onClick = { weekPattern = WeekPattern.ALL },
                        label = { Text(stringResource(R.string.week_pattern_all)) }
                    )
                    FilterChip(
                        selected = weekPattern == WeekPattern.ODD,
                        onClick = { weekPattern = WeekPattern.ODD },
                        label = { Text(stringResource(R.string.week_pattern_odd)) }
                    )
                    FilterChip(
                        selected = weekPattern == WeekPattern.EVEN,
                        onClick = { weekPattern = WeekPattern.EVEN },
                        label = { Text(stringResource(R.string.week_pattern_even)) }
                    )
                }

                // Color Palette Selection
                Text(stringResource(R.string.course_color_label), style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CourseColors.forEach { hexColor ->
                        val color = Color(android.graphics.Color.parseColor(hexColor))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hexColor }
                                .then(
                                    if (selectedColor == hexColor) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && className.isNotBlank() && room.isNotBlank()) {
                        val updatedCourse = Course(
                            id = initialCourse?.id ?: 0L,
                            semesterId = semesterId,
                            name = name.trim(),
                            code = code.trim().ifEmpty { null },
                            dayOfWeek = dayOfWeek,
                            periodStart = minOf(periodStart, periodEnd),
                            periodEnd = maxOf(periodStart, periodEnd),
                            weekFrom = minOf(weekFrom, weekTo),
                            weekTo = maxOf(weekFrom, weekTo),
                            weekPattern = weekPattern,
                            campus = campus.trim().ifEmpty { null },
                            room = room.trim(),
                            className = className.trim(),
                            teacher = teacher.trim().ifEmpty { null },
                            studentCount = studentCountStr.toIntOrNull() ?: 0,
                            nature = nature,
                            examType = examType,
                            color = selectedColor
                        )
                        onSave(updatedCourse)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
