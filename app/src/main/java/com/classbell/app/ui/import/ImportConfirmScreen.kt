package com.classbell.app.ui.import

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classbell.app.R
import com.classbell.app.core.TimeUtils
import com.classbell.app.data.model.Course
import com.classbell.app.data.model.PeriodSlot
import com.classbell.app.ui.schedule.CourseEditDialog
import com.classbell.app.ui.theme.AccentOrange

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.sp
import com.classbell.app.ui.components.ClassBellSecondaryTopBar

@Composable
fun ImportConfirmScreen(
    viewModel: ImportViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stage by viewModel.importStage.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ClassBellSecondaryTopBar(
                title = stringResource(R.string.import_confirm_title),
                onNavigateBack = onNavigateBack
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val currentStage = stage) {
                is ImportStage.Idle -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(60.dp))
                        Text(
                            text = stringResource(R.string.import_idle_prompt),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text(stringResource(R.string.back))
                        }
                    }
                }

                is ImportStage.Parsing -> {
                    val (parsingTitle, parsingDesc) = when (currentStage.type) {
                        ImportType.TIMETABLE -> stringResource(R.string.import_parsing_title_timetable) to stringResource(R.string.import_parsing_desc_timetable)
                        ImportType.CALENDAR -> stringResource(R.string.import_parsing_title_calendar) to stringResource(R.string.import_parsing_desc_calendar)
                        ImportType.PERIOD_TABLE -> stringResource(R.string.import_parsing_title_period_table) to stringResource(R.string.import_parsing_desc_period_table)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(72.dp))
                        CircularProgressIndicator(
                            color = AccentOrange,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = parsingTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = parsingDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }

                is ImportStage.ConfirmTimetable -> {
                    ConfirmTimetableContent(
                        initialCourses = currentStage.courses,
                        onConfirm = { viewModel.saveConfirmedCourses(it) },
                        onCancel = onNavigateBack
                    )
                }

                is ImportStage.ConfirmCalendar -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.import_calendar_extracted, currentStage.semester.name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.import_calendar_w1, currentStage.semester.week1Monday))
                        Text(stringResource(R.string.import_calendar_teaching_weeks, currentStage.semester.teachingFrom, currentStage.semester.teachingTo))
                        Text(stringResource(R.string.import_calendar_holidays_count, currentStage.holidays.size))
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.saveConfirmedCalendar(currentStage.semester, currentStage.holidays) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                        ) {
                            Text(stringResource(R.string.import_calendar_save_btn))
                        }
                    }
                }

                is ImportStage.ConfirmPeriodTable -> {
                    ConfirmPeriodTableContent(
                        initialSlots = currentStage.periodSlots,
                        onConfirm = { viewModel.saveConfirmedPeriodTable(it) },
                        onCancel = onNavigateBack
                    )
                }

                is ImportStage.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(60.dp))
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.import_error_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentStage.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        if (currentStage.canSwitchToGlm) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.resetStage(); onNavigateBack() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.back))
                                }
                                Button(
                                    onClick = { viewModel.switchToGlmAndRetry() },
                                    modifier = Modifier.weight(1.4f),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                                ) {
                                    Text("换用智谱专线重试")
                                }
                            }
                        } else {
                            Button(onClick = { viewModel.resetStage(); onNavigateBack() }) {
                                Text(stringResource(R.string.import_retry_btn))
                            }
                        }
                    }
                }

                is ImportStage.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.import_success_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.import_success_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                viewModel.resetStage()
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                        ) {
                            Text(stringResource(R.string.import_done_btn))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmTimetableContent(
    initialCourses: List<Course>,
    onConfirm: (List<Course>) -> Unit,
    onCancel: () -> Unit
) {
    val courseList = remember { mutableStateListOf<Course>().apply { addAll(initialCourses) } }
    var editingCourseIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.import_courses_detected_format, courseList.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            itemsIndexed(courseList) { index, course ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = course.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${course.className} · ${course.studentCount} " + stringResource(R.string.unit_people),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${TimeUtils.getWeekdayShortName(course.dayOfWeek)} " +
                                    stringResource(R.string.course_period_format, course.periodStart, course.periodEnd) +
                                    " · ${course.room}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentOrange
                            )
                            Text(
                                text = stringResource(R.string.course_detail_weeks) + ": ${course.weekFrom}–${course.weekTo}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row {
                            IconButton(onClick = { editingCourseIndex = index }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                            }
                            IconButton(onClick = { courseList.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.import_discard_btn))
            }

            Button(
                onClick = { onConfirm(courseList) },
                modifier = Modifier.weight(2f),
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                enabled = courseList.isNotEmpty()
            ) {
                Text(stringResource(R.string.import_confirm_btn_format, courseList.size))
            }
        }
    }

    if (editingCourseIndex != null) {
        val index = editingCourseIndex!!
        val course = courseList[index]
        CourseEditDialog(
            initialCourse = course,
            semesterId = course.semesterId,
            onDismiss = { editingCourseIndex = null },
            onSave = { updated ->
                courseList[index] = updated
                editingCourseIndex = null
            }
        )
    }
}

@Composable
fun ConfirmPeriodTableContent(
    initialSlots: List<PeriodSlot>,
    onConfirm: (List<PeriodSlot>) -> Unit,
    onCancel: () -> Unit
) {
    val slotList = remember { mutableStateListOf<PeriodSlot>().apply { addAll(initialSlots) } }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text(
                        text = stringResource(R.string.confirm_period_table_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.confirm_period_table_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            itemsIndexed(slotList) { index, slot ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AccentOrange.copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${slot.order}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentOrange
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = slot.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${slot.startTime} – ${slot.endTime}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { slotList.removeAt(index) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = Color.Red.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.import_discard_btn))
            }

            Button(
                onClick = { onConfirm(slotList) },
                modifier = Modifier.weight(2f),
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                enabled = slotList.isNotEmpty()
            ) {
                Text(stringResource(R.string.confirm_period_table_btn))
            }
        }
    }
}
