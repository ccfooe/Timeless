package com.classbell.app.ui.today

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
import androidx.compose.material3.LinearProgressIndicator
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
import com.classbell.app.ui.components.PureSurface
import com.classbell.app.ui.theme.AccentAmber
import com.classbell.app.ui.theme.AccentOrange
import com.classbell.app.ui.theme.CountdownDigits
import com.classbell.app.ui.theme.LocalAccentColor
import com.classbell.app.ui.theme.SystemGreen
import java.time.Duration
import java.time.LocalTime

@Composable
fun NowInClassCard(
    currentMeeting: MeetingOccurrence?,
    nextMeeting: MeetingOccurrence?,
    currentTime: LocalTime,
    todayAffairs: List<Affair>,
    onToggleAffair: (Affair) -> Unit,
    onAddAffair: () -> Unit,
    onEndClass: () -> Unit,
    onExtendClass: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (currentMeeting != null) {
            // Live In-Class Active Dashboard (Swiss mechanical / Pure Surface)
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
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.status_in_progress),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${currentMeeting.course.room} · ${currentMeeting.course.className}",
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

                    Spacer(modifier = Modifier.height(6.dp))

                    // Huge Digital Countdown Display (Swiss Typography, tight monospace)
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
                        style = CountdownDigits,
                        color = accentColor
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Ultra-slim Progress Track (4dp)
                    LinearProgressIndicator(
                        progress = { currentMeeting.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.15f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Micro Timestamp Row
                    val elapsedMinutes = Duration.between(currentMeeting.startTime, currentTime).toMinutes().coerceAtLeast(0)
                    val elapsedText = if (elapsedMinutes >= 60) {
                        "${elapsedMinutes / 60}h ${elapsedMinutes % 60}m"
                    } else {
                        "${elapsedMinutes}m"
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons (End Class, +5min)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_end_class), style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { onExtendClass(5) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = accentColor
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_extend_5min), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        } else if (nextMeeting != null) {
            // Next class preview card (PureSurface)
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
                        Text(
                            text = stringResource(
                                R.string.next_class_time_room,
                                TimeUtils.formatTime(nextMeeting.startTime),
                                nextMeeting.course.room
                            ),
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

        // Second Widget Card: Today's Tasks (Live synced with Room DB)
        TodayTasksCard(
            affairs = todayAffairs,
            onToggleAffair = onToggleAffair,
            onAddAffair = onAddAffair
        )
    }
}

// Interactive Tasks Widget Card live synced with Room Database
@Composable
fun TodayTasksCard(
    affairs: List<Affair>,
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

                        val timeStr = affair.time?.let { TimeUtils.formatTime(it) } ?: allDayStr
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
