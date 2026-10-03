package com.classbell.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.classbell.app.R
import com.classbell.app.core.TimeUtils
import com.classbell.app.data.model.Affair
import com.classbell.app.data.model.MeetingOccurrence
import com.classbell.app.data.model.SubPeriodKind
import com.classbell.app.data.repository.SettingsRepository
import com.classbell.app.ui.components.PureSurface
import com.classbell.app.ui.theme.AccentAmber
import com.classbell.app.ui.theme.CountdownDigits
import com.classbell.app.ui.theme.LocalAccentColor
import com.classbell.app.ui.theme.SystemGreen
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.Duration
import java.time.LocalTime

@Composable
fun NowInClassCard(
    currentMeeting: MeetingOccurrence?,
    nextMeeting: MeetingOccurrence?,
    currentTime: LocalTime,
    todayAffairs: List<Affair>,
    sessionSuppression: SessionSuppression = SessionSuppression(),
    onToggleAffair: (Affair) -> Unit,
    onAddAffair: () -> Unit,
    onEndClass: () -> Unit,
    onEndPeriod: () -> Unit = onEndClass,
    onEndHalfDay: () -> Unit = onEndClass,
    onEndFullDay: () -> Unit = onEndClass,
    onRestoreTiming: () -> Unit = {},
    onExtendClass: (Int) -> Unit,
    userRole: String = SettingsRepository.ROLE_TEACHER,
    autoTimezone: Boolean = false,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    var showDismissDialog by remember { mutableStateOf(false) }

    if (showDismissDialog) {
        val currentSlotName = currentMeeting?.subPeriodState?.periodLabel ?: "本节课"
        DismissClassOptionsDialog(
            currentSlotName = currentSlotName,
            onDismissRequest = { showDismissDialog = false },
            onEndPeriod = {
                showDismissDialog = false
                onEndPeriod()
            },
            onEndHalfDay = {
                showDismissDialog = false
                onEndHalfDay()
            },
            onEndFullDay = {
                showDismissDialog = false
                onEndFullDay()
            }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (currentMeeting != null) {
            val sub = currentMeeting.subPeriodState
            val isBreak = sub?.isBreak == true
            val isPreClass = sub?.kind == SubPeriodKind.PRE_CLASS
            val activeColor = when {
                isPreClass -> Color(0xFF0077B6)
                isBreak -> AccentAmber
                else -> accentColor
            }

            val targetStartTime = sub?.startTime ?: currentMeeting.startTime
            val targetEndTime = sub?.endTime ?: currentMeeting.endTime
            val activeProgress = if (isPreClass) 0f else (sub?.progress ?: currentMeeting.progress)

            // Frosted Surface Live Activity Card (Low pressure, harmonious, with full progress bar)
            PureSurface(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 20.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Row: Status dot + Course Room & Class ... Current clock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(activeColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isPreClass -> "${stringResource(R.string.status_pre_class)} · ${sub?.periodLabel ?: ""}"
                                    isBreak -> sub?.breakTitle ?: stringResource(R.string.status_recess)
                                    sub != null -> "${stringResource(R.string.status_in_progress)} · ${sub.periodLabel}"
                                    else -> stringResource(R.string.status_in_progress)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = activeColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val currentSub = if (userRole == SettingsRepository.ROLE_STUDENT) {
                                val teacherOrClass = currentMeeting.course.teacher?.takeIf { it.isNotBlank() } ?: currentMeeting.course.className
                                if (teacherOrClass.isNotBlank()) "${currentMeeting.course.room} · $teacherOrClass" else currentMeeting.course.room
                            } else {
                                "${currentMeeting.course.room} · ${currentMeeting.course.className}"
                            }
                            Text(
                                text = currentSub,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Minimalist Hairline Clock Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = TimeUtils.formatTime(currentTime),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Course Name
                    Text(
                        text = currentMeeting.course.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isPreClass) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.pre_class_countdown_format, TimeUtils.formatTime(targetEndTime)),
                            style = MaterialTheme.typography.bodySmall,
                            color = activeColor,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (isBreak && sub?.nextPeriodLabel != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.next_period_starts_at, sub.nextPeriodLabel, TimeUtils.formatTime(sub.endTime)),
                            style = MaterialTheme.typography.bodySmall,
                            color = activeColor,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (sub != null && sub.totalPeriodsInCourse > 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.period_in_course_format, sub.periodLabel, sub.periodIndexInCourse, sub.totalPeriodsInCourse),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Digital Countdown Display
                    val totalRemainingSecs = run {
                        val duration = Duration.between(currentTime, targetEndTime).seconds
                        if (duration > 0) duration else 0L
                    }
                    val hours = totalRemainingSecs / 3600
                    val mins = (totalRemainingSecs % 3600) / 60
                    val secs = totalRemainingSecs % 60
                    val timerStr = if (hours > 0) {
                        String.format("%d:%02d:%02d", hours, mins, secs)
                    } else {
                        String.format("%02d:%02d", mins, secs)
                    }

                    Text(
                        text = timerStr,
                        style = CountdownDigits,
                        color = activeColor
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Fluid Track Progress Bar (Bilibili Player / Lock Screen Unified Aesthetic)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(1.75.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = activeProgress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(1.75.dp))
                                .background(activeColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Micro Timestamp Row (Aligned with Lock Screen & Notification Grammar)
                    val elapsedMinutes = Duration.between(targetStartTime, currentTime).toMinutes().coerceAtLeast(0)
                    val elapsedText = if (elapsedMinutes >= 60) {
                        "${elapsedMinutes / 60}h ${elapsedMinutes % 60}m"
                    } else {
                        "${elapsedMinutes}m"
                    }
                    val percent = (activeProgress * 100).toInt().coerceIn(0, 100)
                    val currentPeriodLabel = sub?.periodLabel ?: "第${currentMeeting.periodStart}节"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = TimeUtils.formatTime(targetStartTime),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when {
                                isPreClass -> stringResource(R.string.status_pre_class)
                                isBreak -> stringResource(R.string.recess_progress_format, percent)
                                else -> "$currentPeriodLabel $percent% · 已上$elapsedText"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = TimeUtils.formatTime(targetEndTime),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons (End Class, +5min)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { if (isPreClass) onEndClass() else showDismissDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.StopCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPreClass) stringResource(R.string.btn_cancel_pre_class) else stringResource(R.string.btn_end_class),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        if (!isPreClass) {
                            OutlinedButton(
                                onClick = { onExtendClass(5) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = activeColor
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBreak) stringResource(R.string.btn_extend_break_5min) else stringResource(R.string.btn_extend_5min),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        } else if (nextMeeting != null) {
            // Next class preview card (Frosted Surface)
            PureSurface(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.next_class_prefix),
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = nextMeeting.course.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        val nextSub = if (userRole == SettingsRepository.ROLE_STUDENT && !nextMeeting.course.teacher.isNullOrBlank()) {
                            "${TimeUtils.formatTime(nextMeeting.startTime)} · ${nextMeeting.course.room} (${nextMeeting.course.teacher})"
                        } else {
                            stringResource(
                                R.string.next_class_time_room,
                                TimeUtils.formatTime(nextMeeting.startTime),
                                nextMeeting.course.room
                            )
                        }
                        Text(
                            text = nextSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.period_range_format,
                                nextMeeting.periodStart,
                                nextMeeting.periodEnd
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (sessionSuppression.isAnySuppressed && currentMeeting == null) {
            val suppressDesc = when {
                sessionSuppression.suppressFullDay -> "今日已开启免打扰，上课计时与提醒已暂停"
                sessionSuppression.suppressMorning -> "今日上午已开启免打扰，上课计时与提醒已暂停"
                sessionSuppression.suppressAfternoon -> "今日下午已开启免打扰，上课计时与提醒已暂停"
                else -> "已提前结束部分课程节次计时"
            }
            PureSurface(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 12.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.DoNotDisturbOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = suppressDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "恢复计时",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onRestoreTiming)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Second Widget Card: Today's Tasks (Live synced with Room DB)
        TodayTasksCard(
            affairs = todayAffairs,
            autoTimezone = autoTimezone,
            onToggleAffair = onToggleAffair,
            onAddAffair = onAddAffair
        )
    }
}

// Interactive Tasks Widget Card live synced with Room Database
@Composable
fun TodayTasksCard(
    affairs: List<Affair>,
    autoTimezone: Boolean = false,
    onToggleAffair: (Affair) -> Unit,
    onAddAffair: () -> Unit
) {
    val doneCount = affairs.count { it.isDone }
    val totalCount = affairs.size
    val accentColor = LocalAccentColor.current

    PureSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 18.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PendingActions,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.today_affairs_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (totalCount > 0) {
                        Text(
                            text = "$doneCount/$totalCount",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    // Minimal circular quick add button
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.12f))
                            .clickable(onClick = onAddAffair),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.affair_add_title),
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (affairs.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onAddAffair)
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.today_no_affairs_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                val allDayStr = stringResource(R.string.affair_all_day)
                affairs.forEach { affair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleAffair(affair) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Minimal hairline check circle
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (affair.isDone) SystemGreen else Color.Transparent
                                    )
                                    .border(
                                        width = 1.2.dp,
                                        color = if (affair.isDone) SystemGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (affair.isDone) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = affair.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (affair.isDone) FontWeight.Normal else FontWeight.Medium,
                                    color = if (affair.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (affair.isDone) TextDecoration.LineThrough else TextDecoration.None
                                )
                                if (!affair.place.isNullOrBlank()) {
                                    Text(
                                        text = affair.place,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        val conv = com.classbell.app.core.TimeUtils.convertAffairDateTime(affair.date, affair.time, autoTimezone)
                        val timeStr = conv.effectiveTime?.let {
                            if (conv.isConverted) "${com.classbell.app.core.TimeUtils.formatTime(it)} (当地)" else com.classbell.app.core.TimeUtils.formatTime(it)
                        } ?: allDayStr
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (affair.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DismissClassOptionsDialog(
    currentSlotName: String,
    onDismissRequest: () -> Unit,
    onEndPeriod: () -> Unit,
    onEndHalfDay: () -> Unit,
    onEndFullDay: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.StopCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "提前结束教学 / 计时",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "请选择本次中止计时的范围（仅对今日临时生效，不影响原课表）：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Option 1: 仅结束当前本小节
                Card(
                    onClick = onEndPeriod,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "提前结束本节课 ($currentSlotName)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "立即停止当前节次倒计时，进入课间休息；下节课按时继续",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Option 2: 结束本半天所有计时
                Card(
                    onClick = onEndHalfDay,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(AccentAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "结束本半天所有课程计时",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "终止当前半天（上午/下午）后续所有课程倒计时与铃声",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Option 3: 结束今日全部计时
                Card(
                    onClick = onEndFullDay,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DoNotDisturbOn, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "结束今日全部计时 (全天免打扰)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "今日后续不再自动运行任何上课程序与悬浮提醒",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}

