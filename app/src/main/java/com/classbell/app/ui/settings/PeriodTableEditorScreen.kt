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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import com.classbell.app.ui.components.ClassBellSecondaryTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classbell.app.R
import com.classbell.app.core.TimeUtils
import com.classbell.app.data.model.PeriodSlot
import com.classbell.app.data.repository.PeriodRepository
import com.classbell.app.ui.theme.AccentOrange
import androidx.compose.runtime.rememberCoroutineScope
import com.classbell.app.ClassBellApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodTableEditorScreen(
    periodRepository: PeriodRepository,
    onNavigateBack: () -> Unit,
    onImportPeriodTable: ((Uri) -> Unit)? = null,
    onNavigateToImportConfirm: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val registryOwner = LocalActivityResultRegistryOwner.current

    val periodPickerLauncher = if (registryOwner != null) {
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                onImportPeriodTable?.invoke(it)
                onNavigateToImportConfirm?.invoke()
            }
        }
    } else null

    val periods by periodRepository.allPeriodSlots.collectAsStateWithLifecycle(initialValue = emptyList())
    var editingSlot by remember { mutableStateOf<PeriodSlot?>(null) }
    var isAddingSlot by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ClassBellSecondaryTopBar(
                title = stringResource(R.string.period_editor_title),
                onNavigateBack = onNavigateBack,
                actions = {
                    if (onImportPeriodTable != null && onNavigateToImportConfirm != null) {
                        TextButton(
                            onClick = {
                                periodPickerLauncher?.launch("image/*")
                                    ?: Toast.makeText(context, context.getString(R.string.storage_unavailable), Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.ai_photo_recognize_period_table),
                                color = AccentOrange,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddingSlot = true },
                containerColor = AccentOrange,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_period_slot))
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
                    text = stringResource(R.string.period_editor_desc),
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
                                color = AccentOrange
                            )
                        }

                        Row {
                            IconButton(onClick = { editingSlot = slot }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                            }
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        periodRepository.deletePeriodSlot(slot)
                                        val app = context.applicationContext as? ClassBellApp
                                        if (app != null) {
                                            ClassBellApp.cachedPeriodSlots = app.database.periodSlotDao().getAllPeriodSlotsSync()
                                        }
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color.Red)
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
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            if (savedSlot.id == 0L) {
                                periodRepository.insertPeriodSlot(savedSlot)
                            } else {
                                periodRepository.updatePeriodSlot(savedSlot)
                            }
                            val app = context.applicationContext as? ClassBellApp
                            if (app != null) {
                                ClassBellApp.cachedPeriodSlots = app.database.periodSlotDao().getAllPeriodSlotsSync()
                            }
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
    val defaultLabel = stringResource(R.string.period_default_label, order)
    var label by remember { mutableStateOf(initialSlot?.label ?: defaultLabel) }
    var startTimeStr by remember { mutableStateOf(initialSlot?.startTime?.toString() ?: "08:30") }
    var endTimeStr by remember { mutableStateOf(initialSlot?.endTime?.toString() ?: "09:15") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialSlot == null) stringResource(R.string.add_period_slot) else stringResource(R.string.edit_period_slot), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = order.toString(),
                    onValueChange = { order = it.toIntOrNull() ?: order },
                    label = { Text(stringResource(R.string.period_slot_order)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.period_slot_name)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = startTimeStr,
                    onValueChange = { startTimeStr = it },
                    label = { Text(stringResource(R.string.period_start_time)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = endTimeStr,
                    onValueChange = { endTimeStr = it },
                    label = { Text(stringResource(R.string.period_end_time)) },
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
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
