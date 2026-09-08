package com.classbell.app.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classbell.app.R
import com.classbell.app.core.TimeUtils
import com.classbell.app.data.model.MeetingOccurrence
import com.classbell.app.ui.glass.GlassCard
import com.classbell.app.ui.theme.AccentAmber
import com.classbell.app.ui.theme.AccentOrange
import com.classbell.app.ui.theme.AccentOrangeSubtle
import com.classbell.app.ui.theme.SystemGreen
import java.time.Duration
import java.time.LocalTime

@Composable
fun NowInClassCard(
    currentMeeting: MeetingOccurrence?,
    nextMeeting: MeetingOccurrence?,
    currentTime: LocalTime,
    onEndClass: () -> Unit,
    onExtendClass: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (currentMeeting != null) {
            // Live Activity Card (Exactly matching the Reference Screenshot)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 8.dp,
                contentPadding = 18.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Row: [ 📕 • 上课中 ] Pill + Current Time [ 08:52 ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Orange Tag Pill: 📕 • 上课中
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentOrangeSubtle)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = AccentOrange,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(AccentOrange)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.status_in_progress),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrange
                            )
                        }

                        // Clock pill badge: 08:52
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = TimeUtils.formatTime(currentTime),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Middle Row: Course Title + Huge Countdown Timer on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentMeeting.course.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${currentMeeting.course.room} · ${currentMeeting.course.className}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Big Bold Digital Countdown (e.g. 1:07:21 or 42:15)
                        val totalRemainingSecs = run {
                            val duration = Duration.between(currentTime, currentMeeting.endTime).seconds
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
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 28.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            fontWeight = FontWeight.Black,
                            color = AccentOrange
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sleek Orange Progress Bar
                    LinearProgressIndicator(
                        progress = { currentMeeting.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AccentOrange,
                        trackColor = AccentOrange.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bottom info: 00:01 / 08:30 ... 已进行 45min · 40% ... 10:00 / 12:00
                    val elapsedMinutes = Duration.between(currentMeeting.startTime, currentTime).toMinutes().coerceAtLeast(0)
                    val elapsedText = if (elapsedMinutes >= 60) {
                        "${elapsedMinutes / 60}h ${elapsedMinutes % 60}min"
                    } else {
                        "${elapsedMinutes}min"
                    }
                    val percent = (currentMeeting.progress * 100).toInt().coerceIn(0, 100)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = TimeUtils.formatTime(currentMeeting.startTime),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.elapsed_format, elapsedText, percent),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = TimeUtils.formatTime(currentMeeting.endTime),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Class Action Buttons (End Class, +5min)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEndClass,
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
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_end_class), style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { onExtendClass(5) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AccentAmber
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_extend_5min), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        } else if (nextMeeting != null) {
            // Next class preview card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp,
                contentPadding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentOrangeSubtle)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.next_class_prefix),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = nextMeeting.course.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${TimeUtils.formatTime(nextMeeting.startTime)} 开始 · ${nextMeeting.course.room}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "第 ${nextMeeting.periodStart}–${nextMeeting.periodEnd} 节",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Second Widget Card: Today's Tasks (Matching Reference Screenshot second card)
        TodayTasksCard()
    }
}

// Interactive Tasks Widget Card from the Reference Screenshot
@Composable
fun TodayTasksCard() {
    data class TaskItem(val title: String, var isDone: Boolean, val timeTag: String)
    val tasks = remember {
        mutableStateListOf(
            TaskItem("统计学生上课到勤人数", false, "00:59"),
            TaskItem("课前准备播放多媒体课件素材", true, "已就绪")
        )
    }

    val doneCount = tasks.count { it.isDone }
    val totalCount = tasks.size

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        contentPadding = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentOrangeSubtle)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PendingActions,
                        contentDescription = null,
                        tint = AccentOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.today_affairs_title),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                }

                Text(
                    text = "$doneCount/$totalCount",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            tasks.forEachIndexed { index, task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            tasks[index] = task.copy(isDone = !task.isDone)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Checkbox circle
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (task.isDone) SystemGreen else Color.Transparent
                                )
                                .then(
                                    if (!task.isDone) {
                                        Modifier.background(Color.Transparent, CircleShape)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (task.isDone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (task.isDone) FontWeight.Normal else FontWeight.Medium,
                            color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = task.timeTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
