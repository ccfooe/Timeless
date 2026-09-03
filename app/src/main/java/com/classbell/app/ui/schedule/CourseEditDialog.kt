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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classbell.app.core.TimeUtils
import com.classbell.app.data.model.Course
import com.classbell.app.data.model.WeekPattern
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
    var periodEnd by remember { mutableIntStateOf(initialCourse?.periodEnd ?: 4) }
    var weekFrom by remember { mutableIntStateOf(initialCourse?.weekFrom ?: 2) }
    var weekTo by remember { mutableIntStateOf(initialCourse?.weekTo ?: 17) }
    var weekPattern by remember { mutableStateOf(initialCourse?.weekPattern ?: WeekPattern.ALL) }
    var campus by remember { mutableStateOf(initialCourse?.campus ?: "金牛校区") }
    var room by remember { mutableStateOf(initialCourse?.room ?: "贡院401") }
    var className by remember { mutableStateOf(initialCourse?.className ?: "") }
    var studentCountStr by remember { mutableStateOf(initialCourse?.studentCount?.toString() ?: "45") }
    var nature by remember { mutableStateOf(initialCourse?.nature ?: "必修") }
    var examType by remember { mutableStateOf(initialCourse?.examType ?: "考试") }
    var selectedColor by remember { mutableStateOf(initialCourse?.color ?: CourseColors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCourse == null) "新增课程" else "编辑课程",
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
                    label = { Text("课程名称 *") },
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
                        label = { Text("授课班级 *") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )

                    OutlinedTextField(
                        value = studentCountStr,
                        onValueChange = { studentCountStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("人数") },
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
                        label = { Text("校区") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("教室 *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Day of Week Selector
                Text("星期: ${TimeUtils.getWeekdayShortName(dayOfWeek)}", style = MaterialTheme.typography.labelMedium)
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
                        label = { Text("起始节次 (1-10)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = periodEnd.toString(),
                        onValueChange = { periodEnd = it.toIntOrNull()?.coerceIn(1, 10) ?: periodEnd },
                        label = { Text("结束节次 (1-10)") },
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
                        label = { Text("起始周") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weekTo.toString(),
                        onValueChange = { weekTo = it.toIntOrNull()?.coerceAtLeast(1) ?: weekTo },
                        label = { Text("结束周") },
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
                        label = { Text("每周") }
                    )
                    FilterChip(
                        selected = weekPattern == WeekPattern.ODD,
                        onClick = { weekPattern = WeekPattern.ODD },
                        label = { Text("单周") }
                    )
                    FilterChip(
                        selected = weekPattern == WeekPattern.EVEN,
                        onClick = { weekPattern = WeekPattern.EVEN },
                        label = { Text("双周") }
                    )
                }

                // Color Palette Selection
                Text("课程颜色", style = MaterialTheme.typography.labelMedium)
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
                            studentCount = studentCountStr.toIntOrNull() ?: 0,
                            nature = nature,
                            examType = examType,
                            color = selectedColor
                        )
                        onSave(updatedCourse)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
