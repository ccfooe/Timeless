package com.classbell.app.ui.calendar

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.window.Dialog
import com.classbell.app.R
import com.classbell.app.data.model.Course
import com.classbell.app.data.model.Override
import com.classbell.app.data.model.OverrideKind
import com.classbell.app.ui.theme.AccentOrange
import com.classbell.app.ui.theme.LocalAccentColor
import java.time.LocalDate

@Composable
fun ShiftCourseDialog(
    targetDate: LocalDate,
    courses: List<Course>,
    currentOverrides: List<Override>,
    onSaveWholeDayShift: (targetDate: LocalDate, followsWeekday: Int, note: String?) -> Unit,
    onSaveWholeDayCancel: (targetDate: LocalDate, note: String?) -> Unit,
    onSaveSingleAdjustment: (targetDate: LocalDate, courseId: Long, pStart: Int, pEnd: Int, room: String?, note: String?) -> Unit,
    onSaveSingleCancel: (targetDate: LocalDate, courseId: Long, note: String?) -> Unit,
    onClearOverrides: (targetDate: LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val accentColor = LocalAccentColor.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 整天调课, 1: 单课调整

    // Whole day state
    val existingWholeDayShift = currentOverrides.firstOrNull { it.kind == OverrideKind.SHIFT && it.courseId == null }
    val existingWholeDayCancel = currentOverrides.firstOrNull { it.kind == OverrideKind.CANCEL && it.courseId == null }

    var wholeDayMode by remember {
        mutableStateOf(if (existingWholeDayCancel != null) "CANCEL" else "SHIFT")
    }
    var followsWeekday by remember {
        mutableIntStateOf(existingWholeDayShift?.followsWeekday ?: 5) // default Friday
    }
    var wholeDayNote by remember {
        mutableStateOf(existingWholeDayShift?.note ?: existingWholeDayCancel?.note ?: "")
    }

    // Single course state
    var selectedCourseId by remember {
        mutableStateOf(courses.firstOrNull()?.id ?: 0L)
    }
    val selectedCourse = courses.firstOrNull { it.id == selectedCourseId }
    var singleMode by remember { mutableStateOf("ADJUST") } // "ADJUST" or "CANCEL"
    var targetPeriodStart by remember(selectedCourseId) {
        mutableIntStateOf(selectedCourse?.periodStart ?: 1)
    }
    var targetPeriodEnd by remember(selectedCourseId) {
        mutableIntStateOf(selectedCourse?.periodEnd ?: 2)
    }
    var targetRoom by remember(selectedCourseId) {
        mutableStateOf(selectedCourse?.room ?: "")
    }
    var singleNote by remember { mutableStateOf("") }
    var courseDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = AccentOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "学校调课设置",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        val weekdayCn = when (targetDate.dayOfWeek.value) {
                            1 -> "一"; 2 -> "二"; 3 -> "三"; 4 -> "四"; 5 -> "五"; 6 -> "六"; else -> "日"
                        }
                        Text(
                            text = "${targetDate.year}年${targetDate.monthValue}月${targetDate.dayOfMonth}日 (周$weekdayCn)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Overrides Banner
                if (currentOverrides.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentOrange.copy(alpha = 0.12f))
                            .border(1.dp, AccentOrange.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "当前日期已设调课规则",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentOrange
                                )
                                TextButton(
                                    onClick = {
                                        onClearOverrides(targetDate)
                                        android.widget.Toast.makeText(context, "已清除该日调课，恢复正常课表", android.widget.Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("清除调课并恢复", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            currentOverrides.forEach { ov ->
                                val desc = when (ov.kind) {
                                    OverrideKind.SHIFT -> {
                                        val dayName = when (ov.followsWeekday) {
                                            1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"; 5 -> "周五"; 6 -> "周六"; else -> "周日"
                                        }
                                        "全天按【$dayName】课表行课" + (ov.note?.let { " ($it)" } ?: "")
                                    }
                                    OverrideKind.CANCEL -> {
                                        if (ov.courseId == null) "全天停课放假" + (ov.note?.let { " ($it)" } ?: "")
                                        else {
                                            val cName = courses.firstOrNull { it.id == ov.courseId }?.name ?: "课程"
                                            "【$cName】单课停课"
                                        }
                                    }
                                    OverrideKind.COURSE_ADJUST -> {
                                        val cName = courses.firstOrNull { it.id == ov.courseId }?.name ?: "课程"
                                        "【$cName】改至第${ov.targetPeriodStart}-${ov.targetPeriodEnd}节 · ${ov.targetRoom ?: ""}"
                                    }
                                }
                                Text(
                                    text = "• $desc",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Tabs: 0 整天调课, 1 单门课程调整
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = accentColor
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("整天调课 (最常见)", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("单课调换/补课", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // TAB 0: 整天调课
                    Text(
                        text = "调课类型",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { wholeDayMode = "SHIFT" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (wholeDayMode == "SHIFT") accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (wholeDayMode == "SHIFT") accentColor.copy(alpha = 0.12f) else Color.Transparent
                            )
                        ) {
                            Text(
                                "按某天课表行课",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (wholeDayMode == "SHIFT") accentColor else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedButton(
                            onClick = { wholeDayMode = "CANCEL" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (wholeDayMode == "CANCEL") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (wholeDayMode == "CANCEL") MaterialTheme.colorScheme.error.copy(alpha = 0.12f) else Color.Transparent
                            )
                        ) {
                            Text(
                                "全天停课/调休",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (wholeDayMode == "CANCEL") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (wholeDayMode == "SHIFT") {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "选择按星期几的完整课表行课：",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val days = listOf(1 to "周一", 2 to "周二", 3 to "周三", 4 to "周四", 5 to "周五", 6 to "周六", 7 to "周日")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            days.forEach { (d, label) ->
                                val isSelected = followsWeekday == d
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) AccentOrange else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { followsWeekday = d }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = wholeDayNote,
                        onValueChange = { wholeDayNote = it },
                        label = { Text("调课备注 (选填，如：按周五上课 / 国庆调休)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (wholeDayMode == "SHIFT") {
                                onSaveWholeDayShift(targetDate, followsWeekday, wholeDayNote.ifBlank { null })
                            } else {
                                onSaveWholeDayCancel(targetDate, wholeDayNote.ifBlank { null })
                            }
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                    ) {
                        Text("保存整天调课规则", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                } else {
                    // TAB 1: 单门课程调整
                    if (courses.isEmpty()) {
                        Text(
                            text = "当前学期暂无课程，请先在课表添加课程",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Text(
                            text = "选择要调整的课程：",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Course selector dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { courseDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedCourse?.let { "${it.name} (周${it.dayOfWeek}第${it.periodStart}-${it.periodEnd}节)" } ?: "请选择课程",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = courseDropdownExpanded,
                                onDismissRequest = { courseDropdownExpanded = false }
                            ) {
                                courses.forEach { course ->
                                    DropdownMenuItem(
                                        text = { Text("${course.name} · 周${course.dayOfWeek}第${course.periodStart}-${course.periodEnd}节 (${course.room})") },
                                        onClick = {
                                            selectedCourseId = course.id
                                            targetPeriodStart = course.periodStart
                                            targetPeriodEnd = course.periodEnd
                                            targetRoom = course.room
                                            courseDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Adjustment kind
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { singleMode = "ADJUST" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (singleMode == "ADJUST") accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    )
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (singleMode == "ADJUST") accentColor.copy(alpha = 0.12f) else Color.Transparent
                                )
                            ) {
                                Text(
                                    "改期/换节次/补课",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (singleMode == "ADJUST") accentColor else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            OutlinedButton(
                                onClick = { singleMode = "CANCEL" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (singleMode == "CANCEL") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    )
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (singleMode == "CANCEL") MaterialTheme.colorScheme.error.copy(alpha = 0.12f) else Color.Transparent
                                )
                            ) {
                                Text(
                                    "该课程今日停课",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (singleMode == "CANCEL") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (singleMode == "ADJUST") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = targetPeriodStart.toString(),
                                    onValueChange = { targetPeriodStart = it.toIntOrNull()?.coerceIn(1, 12) ?: 1 },
                                    label = { Text("起始节次") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = targetPeriodEnd.toString(),
                                    onValueChange = { targetPeriodEnd = it.toIntOrNull()?.coerceIn(1, 12) ?: 2 },
                                    label = { Text("结束节次") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = targetRoom,
                                onValueChange = { targetRoom = it },
                                label = { Text("调整后教室 (选填)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = singleNote,
                            onValueChange = { singleNote = it },
                            label = { Text("单课备注 (选填，如：补周二第3-4节)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                if (selectedCourseId != 0L) {
                                    if (singleMode == "ADJUST") {
                                        onSaveSingleAdjustment(
                                            targetDate,
                                            selectedCourseId,
                                            targetPeriodStart,
                                            targetPeriodEnd,
                                            targetRoom.ifBlank { null },
                                            singleNote.ifBlank { null }
                                        )
                                    } else {
                                        onSaveSingleCancel(targetDate, selectedCourseId, singleNote.ifBlank { null })
                                    }
                                }
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                        ) {
                            Text("保存单课调整", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                if (currentOverrides.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {
                            onClearOverrides(targetDate)
                            android.widget.Toast.makeText(context, "已清除该日所有调课，恢复正常课表", android.widget.Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("清除该日所有调课规则 (恢复原课表)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
