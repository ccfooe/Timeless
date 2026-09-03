package com.classbell.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classbell.app.core.TimeUtils
import com.classbell.app.data.model.PeriodSlot
import com.classbell.app.data.repository.PeriodRepository
import com.classbell.app.ui.theme.PinkPrimary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodTableEditorScreen(
    periodRepository: PeriodRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val periods by periodRepository.allPeriodSlots.collectAsStateWithLifecycle(initialValue = emptyList())
    var editingSlot by remember { mutableStateOf<PeriodSlot?>(null) }
    var isAddingSlot by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("作息时间表设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddingSlot = true },
                containerColor = PinkPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "新增节次")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "每所大学的上课节次与作息时间各有不同。默认已为您预置四川电影电视学院权威作息（10节课），您可按需增减或修改时间。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(periods) { slot ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = slot.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${TimeUtils.formatTime(slot.startTime)} — ${TimeUtils.formatTime(slot.endTime)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PinkPrimary
                            )
                        }

                        Row {
                            IconButton(onClick = { editingSlot = slot }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑")
                            }
                            IconButton(onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    periodRepository.deletePeriodSlot(slot)
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }

        // Edit/Add Dialog
        if (editingSlot != null || isAddingSlot) {
            val target = editingSlot
            PeriodEditDialog(
                initialSlot = target,
                nextOrder = (periods.maxOfOrNull { it.order } ?: 0) + 1,
                onDismiss = {
                    editingSlot = null
                    isAddingSlot = false
                },
                onSave = { savedSlot ->
                    CoroutineScope(Dispatchers.IO).launch {
                        if (savedSlot.id == 0L) {
                            periodRepository.insertPeriodSlot(savedSlot)
                        } else {
                            periodRepository.updatePeriodSlot(savedSlot)
                        }
                        editingSlot = null
                        isAddingSlot = false
                    }
                }
            )
        }
    }
}

@Composable
fun PeriodEditDialog(
    initialSlot: PeriodSlot?,
    nextOrder: Int,
    onDismiss: () -> Unit,
    onSave: (PeriodSlot) -> Unit
) {
    var order by remember { mutableIntStateOf(initialSlot?.order ?: nextOrder) }
    var label by remember { mutableStateOf(initialSlot?.label ?: "第${order}节") }
    var startTimeStr by remember { mutableStateOf(initialSlot?.startTime?.toString() ?: "08:30") }
    var endTimeStr by remember { mutableStateOf(initialSlot?.endTime?.toString() ?: "09:15") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialSlot == null) "新增节次" else "编辑节次", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = order.toString(),
                    onValueChange = { order = it.toIntOrNull() ?: order },
                    label = { Text("节次序号 (例如 1, 2, 3...)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("显示名称 (例如 第1节)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = startTimeStr,
                    onValueChange = { startTimeStr = it },
                    label = { Text("上课时间 (HH:MM)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = endTimeStr,
                    onValueChange = { endTimeStr = it },
                    label = { Text("下课时间 (HH:MM)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sTime = runCatching { LocalTime.parse(startTimeStr) }.getOrDefault(LocalTime.of(8, 30))
                    val eTime = runCatching { LocalTime.parse(endTimeStr) }.getOrDefault(LocalTime.of(9, 15))
                    val slot = PeriodSlot(
                        id = initialSlot?.id ?: 0L,
                        order = order,
                        label = label.trim(),
                        startTime = sTime,
                        endTime = eTime
                    )
                    onSave(slot)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
