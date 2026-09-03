package com.classbell.app.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classbell.app.data.model.Affair
import com.classbell.app.data.model.AffairCategory
import com.classbell.app.data.model.ReminderChoice
import com.classbell.app.ui.theme.PinkPrimary
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun AffairEditDialog(
    initialAffair: Affair?,
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Affair) -> Unit
) {
    var title by remember { mutableStateOf(initialAffair?.title ?: "") }
    var dateStr by remember { mutableStateOf((initialAffair?.date ?: defaultDate).toString()) }
    var timeStr by remember {
        mutableStateOf(
            initialAffair?.time?.let {
                "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}"
            } ?: "09:00"
        )
    }
    var place by remember { mutableStateOf(initialAffair?.place ?: "") }
    var note by remember { mutableStateOf(initialAffair?.note ?: "") }
    var category by remember { mutableStateOf(initialAffair?.category ?: AffairCategory.AFFAIR) }
    var reminder by remember { mutableStateOf(initialAffair?.reminder ?: ReminderChoice.AT_TIME) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialAffair == null) "新增事务" else "编辑事务",
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
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("事务标题 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("日期 (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )

                    OutlinedTextField(
                        value = timeStr,
                        onValueChange = { timeStr = it },
                        label = { Text("时间 (HH:MM)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = place,
                    onValueChange = { place = it },
                    label = { Text("地点 (可选)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注说明 (可选)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Chips
                Text("分类标签", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        AffairCategory.MEETING to "会议",
                        AffairCategory.EXAM to "考试",
                        AffairCategory.AFFAIR to "事务",
                        AffairCategory.OTHER to "其他"
                    ).forEach { (cat, label) ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(label) }
                        )
                    }
                }

                // Reminder Choice Chips
                Text("提醒时间", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        ReminderChoice.NONE to "不提醒",
                        ReminderChoice.AT_TIME to "准时",
                        ReminderChoice.MIN10 to "10分钟前",
                        ReminderChoice.HOUR1 to "1小时前",
                        ReminderChoice.DAY1 to "1天前"
                    ).forEach { (rem, label) ->
                        FilterChip(
                            selected = reminder == rem,
                            onClick = { reminder = rem },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val parsedDate = runCatching { LocalDate.parse(dateStr) }.getOrDefault(defaultDate)
                        val parsedTime = runCatching { LocalTime.parse(timeStr) }.getOrNull()

                        val affair = Affair(
                            id = initialAffair?.id ?: 0L,
                            title = title.trim(),
                            date = parsedDate,
                            time = parsedTime,
                            place = place.trim().ifEmpty { null },
                            note = note.trim().ifEmpty { null },
                            category = category,
                            reminder = reminder
                        )
                        onSave(affair)
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
