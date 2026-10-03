package com.classbell.app.ui.calendar

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.classbell.app.R
import com.classbell.app.data.model.Affair
import com.classbell.app.data.model.AffairCategory
import com.classbell.app.data.model.ReminderChoice
import com.classbell.app.llm.ParsedAffair
import com.classbell.app.ui.theme.LocalAccentColor
import com.classbell.app.ui.theme.LocalAccentColorSubtle
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AffairEditDialog(
    initialAffair: Affair?,
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Affair) -> Unit,
    onSmartParse: (suspend (String) -> Result<ParsedAffair>)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val accentColor = LocalAccentColor.current
    val accentColorSubtle = LocalAccentColorSubtle.current

    var rawSmartText by remember { mutableStateOf("") }
    var isParsing by remember { mutableStateOf(false) }
    var isSmartCardExpanded by remember { mutableStateOf(initialAffair == null) }

    var title by remember { mutableStateOf(initialAffair?.title ?: "") }
    var selectedDate by remember { mutableStateOf(initialAffair?.date ?: defaultDate) }
    var selectedTime by remember { mutableStateOf(initialAffair?.time ?: LocalTime.of(9, 0)) }
    var place by remember { mutableStateOf(initialAffair?.place ?: "") }
    var note by remember { mutableStateOf(initialAffair?.note ?: "") }
    var category by remember { mutableStateOf(initialAffair?.category ?: AffairCategory.AFFAIR) }
    var reminder by remember { mutableStateOf(initialAffair?.reminder ?: ReminderChoice.AT_TIME) }
    var isDone by remember { mutableStateOf(initialAffair?.isDone ?: false) }

    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    // Date Picker Dialog (Material 3 Scrollable & Calendar Grid)
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confirm), fontWeight = FontWeight.Bold, color = accentColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = true
            )
        }
    }

    // Time Picker Dialog (Material 3 Interactive Clock Wheel / Dial)
    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        showTimePickerDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confirm), fontWeight = FontWeight.Bold, color = accentColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.affair_time_select), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            shape = RoundedCornerShape(26.dp)
        )
    }

    // Modal Dialog Card (ColorOS 16 / Apple Native Design)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 18.dp, bottom = 16.dp, start = 18.dp, end = 18.dp)
            ) {
                // Header with Icon and Close Button (Fixed at top)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentColorSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (initialAffair == null) stringResource(R.string.affair_add_title) else stringResource(R.string.affair_edit_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.affair_dialog_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Form Area (Scrolls freely and never gets covered)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Taobao-style AI Smart Recognition Card (智能填单卡片)
                    if (onSmartParse != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Header row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { isSmartCardExpanded = !isSmartCardExpanded }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(accentColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.affair_smart_paste_title),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.affair_smart_paste_tip),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1
                                        )
                                    }

                                // Expand/Collapse Toggle
                                IconButton(
                                    onClick = { isSmartCardExpanded = !isSmartCardExpanded },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSmartCardExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            AnimatedVisibility(visible = isSmartCardExpanded) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = rawSmartText,
                                        onValueChange = { rawSmartText = it },
                                        placeholder = {
                                            Text(
                                                stringResource(R.string.affair_smart_paste_placeholder),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        },
                                        minLines = 2,
                                        maxLines = 4,
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = accentColor,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Buttons row: [Paste Clipboard] [Clear] [Recognize Button]
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            // Clipboard paste button
                                            TextButton(
                                                onClick = {
                                                    val clipText = clipboardManager.getText()?.text
                                                    if (!clipText.isNullOrBlank()) {
                                                        rawSmartText = clipText.trim()
                                                    } else {
                                                        Toast.makeText(context, context.getString(R.string.affair_smart_paste_empty_clipboard), Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentPaste,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = accentColor
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    stringResource(R.string.affair_smart_paste_clipboard),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = accentColor,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            if (rawSmartText.isNotBlank()) {
                                                TextButton(
                                                    onClick = { rawSmartText = "" },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        stringResource(R.string.affair_smart_paste_clear),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        // AI Recognize Button
                                        Button(
                                            onClick = {
                                                if (rawSmartText.isBlank()) {
                                                    Toast.makeText(context, context.getString(R.string.affair_smart_paste_empty_text), Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                isParsing = true
                                                coroutineScope.launch {
                                                    val parseResult = onSmartParse(rawSmartText)
                                                    isParsing = false
                                                    if (parseResult.isSuccess) {
                                                        val parsed = parseResult.getOrThrow()
                                                        if (parsed.title.isNotBlank()) {
                                                            title = parsed.title.trim()
                                                        }
                                                        parsed.date?.takeIf { it.isNotBlank() }?.let { dStr ->
                                                            runCatching { LocalDate.parse(dStr.trim()) }.getOrNull()?.let {
                                                                selectedDate = it
                                                            }
                                                        }
                                                        parsed.time?.takeIf { it.isNotBlank() }?.let { tStr ->
                                                            runCatching {
                                                                val parts = tStr.trim().split(":")
                                                                if (parts.size >= 2) {
                                                                    LocalTime.of(parts[0].toInt(), parts[1].toInt())
                                                                } else null
                                                            }.getOrNull()?.let {
                                                                selectedTime = it
                                                            }
                                                        }
                                                        parsed.place?.takeIf { it.isNotBlank() }?.let {
                                                            place = it.trim()
                                                        }
                                                        parsed.category?.takeIf { it.isNotBlank() }?.let { catStr ->
                                                            category = when (catStr.trim().uppercase()) {
                                                                "MEETING", "会议" -> AffairCategory.MEETING
                                                                "EXAM", "考试" -> AffairCategory.EXAM
                                                                "AFFAIR", "事务" -> AffairCategory.AFFAIR
                                                                "OTHER", "其他" -> AffairCategory.OTHER
                                                                else -> category
                                                            }
                                                        }
                                                        parsed.note?.takeIf { it.isNotBlank() }?.let {
                                                            note = it.trim()
                                                        }
                                                        Toast.makeText(context, context.getString(R.string.affair_smart_paste_success), Toast.LENGTH_SHORT).show()
                                                        isSmartCardExpanded = false
                                                    } else {
                                                        val errMsg = parseResult.exceptionOrNull()?.message ?: "解析失败"
                                                        Toast.makeText(context, "识别失败: $errMsg", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            enabled = rawSmartText.isNotBlank() && !isParsing,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = accentColor,
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            if (isParsing) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    color = Color.White,
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    stringResource(R.string.affair_smart_paste_parsing),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    stringResource(R.string.affair_smart_paste_btn),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Completion Status Card (for existing affairs)
                if (initialAffair != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { isDone = !isDone },
                        color = if (isDone) Color(0xFF34C759).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isDone) Color(0xFF34C759).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isDone) Color(0xFF34C759) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isDone) stringResource(R.string.affair_status_done) else stringResource(R.string.affair_status_todo),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDone) Color(0xFF248A3D) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Switch(
                                checked = isDone,
                                onCheckedChange = { isDone = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF34C759)
                                )
                            )
                        }
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.affair_title_label)) },
                    placeholder = { Text(stringResource(R.string.affair_title_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Date & Time Interactive Picker Tiles (Award-winning Apple/ColorOS card style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Date Picker Tile
                    val isEn = java.util.Locale.getDefault().language == "en"
                    val weekdayDisplay = if (isEn) {
                        selectedDate.dayOfWeek.name.take(3)
                    } else {
                        when (selectedDate.dayOfWeek.value) {
                            1 -> "周一"
                            2 -> "周二"
                            3 -> "周三"
                            4 -> "周四"
                            5 -> "周五"
                            6 -> "周六"
                            else -> "周日"
                        }
                    }
                    val dateFormatted = if (isEn) {
                        "${selectedDate.month.name.take(3)} ${selectedDate.dayOfMonth}, ${selectedDate.year}"
                    } else {
                        "${selectedDate.year}年${selectedDate.monthValue}月${selectedDate.dayOfMonth}日"
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .clickable { showDatePickerDialog = true }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.affair_date_select),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(accentColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = weekdayDisplay,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = dateFormatted,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Time Picker Tile
                    val timeFormatted = String.format("%02d:%02d", selectedTime.hour, selectedTime.minute)
                    val periodLabel = if (isEn) {
                        if (selectedTime.hour < 12) "AM" else "PM"
                    } else {
                        if (selectedTime.hour < 12) "上午" else if (selectedTime.hour < 18) "下午" else "晚上"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .clickable { showTimePickerDialog = true }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.affair_time_select),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = periodLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = timeFormatted,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                }

                // Place Input
                OutlinedTextField(
                    value = place,
                    onValueChange = { place = it },
                    label = { Text(stringResource(R.string.affair_location_label)) },
                    placeholder = { Text(stringResource(R.string.affair_location_placeholder)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selection with Colored Pills
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.affair_category_label), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(AffairCategory.MEETING, stringResource(R.string.category_meeting), Color(0xFF3A86FF)),
                            Triple(AffairCategory.EXAM, stringResource(R.string.category_exam), Color(0xFFE63946)),
                            Triple(AffairCategory.AFFAIR, stringResource(R.string.category_affair), accentColor),
                            Triple(AffairCategory.OTHER, stringResource(R.string.category_other), Color(0xFF8338EC))
                        ).forEach { (cat, label, color) ->
                            val isSelected = category == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) color.copy(alpha = 0.18f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { category = cat }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Reminder Selection (FlowRow so chips never wrap vertically)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.affair_reminder_label), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ReminderChoice.NONE to stringResource(R.string.reminder_none),
                            ReminderChoice.AT_TIME to stringResource(R.string.reminder_at_time),
                            ReminderChoice.MIN10 to stringResource(R.string.reminder_min10),
                            ReminderChoice.HOUR1 to stringResource(R.string.reminder_hour1),
                            ReminderChoice.DAY1 to stringResource(R.string.reminder_day1)
                        ).forEach { (rem, label) ->
                            val isSelected = reminder == rem
                            FilterChip(
                                selected = isSelected,
                                onClick = { reminder = rem },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor.copy(alpha = 0.15f),
                                    selectedLabelColor = accentColor
                                )
                            )
                        }
                    }
                }

                // Note Input
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.affair_notes_label)) },
                    placeholder = { Text(stringResource(R.string.affair_notes_placeholder)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Notes,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons (Full-width modern button bar - pinned at bottom, never cut off)
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val affair = Affair(
                                id = initialAffair?.id ?: 0L,
                                title = title.trim(),
                                date = selectedDate,
                                time = selectedTime,
                                place = place.trim().ifEmpty { null },
                                note = note.trim().ifEmpty { null },
                                category = category,
                                reminder = reminder,
                                isDone = isDone
                            )
                            onSave(affair)
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (initialAffair == null) stringResource(R.string.affair_save_btn) else stringResource(R.string.affair_update_btn),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
}
