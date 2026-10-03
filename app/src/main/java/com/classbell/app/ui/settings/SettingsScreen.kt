package com.classbell.app.ui.settings

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.classbell.app.ClassBellApp
import com.classbell.app.data.repository.SettingsRepository
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CoPresent
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classbell.app.R
import com.classbell.app.data.backup.ImportMode
import com.classbell.app.ui.import.ImportViewModel
import com.classbell.app.ui.theme.AccentAmber
import com.classbell.app.ui.theme.AccentOrange
import com.classbell.app.ui.theme.AppAccentColor
import com.classbell.app.ui.theme.LocalAccentColor

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    importViewModel: ImportViewModel,
    onNavigateToPeriodEditor: () -> Unit,
    onNavigateToImportConfirm: () -> Unit,
    onNavigateToUserGuide: () -> Unit,
    onNavigateToAppearance: (Int) -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToSurvivalGuide: () -> Unit,
    onNavigateToLlmSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accentColor = LocalAccentColor.current
    val context = LocalContext.current
    val registryOwner = LocalActivityResultRegistryOwner.current

    // File Picker for JSON Backup Import (Safely guarded)
    val jsonPickerLauncher = if (registryOwner != null) {
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                runCatching {
                    context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                        val jsonContent = reader.readText()
                        val app = context.applicationContext as? ClassBellApp
                        if (app != null) {
                            viewModel.importBackup(
                                jsonString = jsonContent,
                                context = context,
                                database = app.database,
                                alarmScheduler = app.alarmScheduler,
                                scheduleRepository = app.scheduleRepository,
                                affairRepository = app.affairRepository
                            ) { success ->
                                val msg = if (success) context.getString(R.string.backup_success) else context.getString(R.string.backup_failed)
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }.onFailure {
                    Toast.makeText(context, context.getString(R.string.file_read_error, it.message ?: ""), Toast.LENGTH_SHORT).show()
                }
            }
        }
    } else null

    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }

    val createDocumentLauncher = if (registryOwner != null) {
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json")
        ) { uri: Uri? ->
            uri?.let { targetUri ->
                val jsonToSave = pendingExportJson
                if (jsonToSave != null) {
                    runCatching {
                        context.contentResolver.openOutputStream(targetUri)?.use { out ->
                            out.write(jsonToSave.toByteArray(Charsets.UTF_8))
                        }
                        Toast.makeText(context, context.getString(R.string.export_save_success), Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(context, context.getString(R.string.export_save_failed, err.message ?: ""), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            pendingExportJson = null
        }
    } else null

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_CALENDAR] == true &&
                permissions[Manifest.permission.WRITE_CALENDAR] == true
        if (granted) {
            val app = context.applicationContext as? ClassBellApp
            app?.database?.let { db ->
                viewModel.setSyncSystemCalendar(true, context, db) {}
            }
            Toast.makeText(context, context.getString(R.string.sync_system_calendar_toast_synced), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, context.getString(R.string.sync_system_calendar_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    val shareBackupFile: (String) -> Unit = { jsonStr ->
        runCatching {
            val backupDir = java.io.File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
            val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val fileName = "Timeless_Backup_$timeStamp.json"
            val backupFile = java.io.File(backupDir, fileName)
            backupFile.writeText(jsonStr, Charsets.UTF_8)

            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                clipData = ClipData.newRawUri("backup", contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, context.getString(R.string.share_backup_chooser)).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val resInfoList = context.packageManager.queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(chooser)
        }.onFailure { err ->
            Toast.makeText(context, context.getString(R.string.export_save_failed, err.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    var showWarnMinutesDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black
            )
        }

        // Role / Identity Switcher
        item {
            RoleSelectorCard(
                currentRole = state.userRole,
                onRoleSelected = { viewModel.updateUserRole(it) },
                accentColor = accentColor
            )
        }

        // Section 1: 核心作息管理 / Schedule & Period Management
        item {
            SectionHeader(title = stringResource(R.string.settings_section_schedule_mgmt))
        }

        item {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.DateRange,
                    iconColor = Color(0xFF4361EE),
                    title = stringResource(R.string.period_table_manager),
                    subtitle = stringResource(R.string.period_table_manager_sub),
                    onClick = onNavigateToPeriodEditor
                )
                SettingsDivider()
                SettingsItemWithSwitch(
                    icon = Icons.Default.Schedule,
                    iconColor = Color(0xFF009688),
                    title = stringResource(R.string.timezone_auto_convert_title),
                    subtitle = if (state.autoTimezoneConversion) {
                        stringResource(R.string.timezone_auto_convert_sub_on, com.classbell.app.core.TimeUtils.getCurrentZoneDisplay())
                    } else {
                        stringResource(R.string.timezone_auto_convert_sub_off)
                    },
                    checked = state.autoTimezoneConversion,
                    onCheckedChange = { viewModel.setAutoTimezoneConversion(it) }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    iconColor = accentColor,
                    title = stringResource(R.string.warn_minutes_title),
                    subtitle = stringResource(R.string.warn_minutes_sub, state.endWarnMinutes),
                    onClick = { showWarnMinutesDialog = true }
                )
            }
        }

        // Section 2: 个性化与偏好 / Appearance & General Preferences
        item {
            SectionHeader(title = stringResource(R.string.settings_section_appearance_pref))
        }

        item {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    iconColor = accentColor,
                    title = stringResource(R.string.theme_mode_title),
                    subtitle = when (state.themeMode) {
                        "light" -> stringResource(R.string.theme_mode_light)
                        "dark" -> stringResource(R.string.theme_mode_dark)
                        else -> stringResource(R.string.theme_mode_system)
                    },
                    onClick = { onNavigateToAppearance(1) }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Palette,
                    iconColor = accentColor,
                    title = stringResource(R.string.accent_color_title),
                    subtitle = stringResource(AppAccentColor.fromKey(state.accentColor).titleRes),
                    onClick = { onNavigateToAppearance(0) }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Public,
                    iconColor = accentColor,
                    title = stringResource(R.string.language_setting),
                    subtitle = if (state.appLanguage == "en") "English" else "简体中文",
                    onClick = { onNavigateToAppearance(2) }
                )
                SettingsDivider()
                SettingsItemWithSwitch(
                    icon = Icons.Default.CalendarMonth,
                    iconColor = Color(0xFF5C6BC0),
                    title = stringResource(R.string.sync_system_calendar_title),
                    subtitle = if (state.syncSystemCalendar) {
                        stringResource(R.string.sync_system_calendar_sub_on)
                    } else {
                        stringResource(R.string.sync_system_calendar_sub_off)
                    },
                    checked = state.syncSystemCalendar,
                    onCheckedChange = { isChecked ->
                        val app = context.applicationContext as? ClassBellApp
                        val db = app?.database
                        if (db != null) {
                            viewModel.setSyncSystemCalendar(
                                enabled = isChecked,
                                context = context,
                                database = db,
                                onPermissionNeeded = {
                                    calendarPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_CALENDAR,
                                            Manifest.permission.WRITE_CALENDAR
                                        )
                                    )
                                }
                            )
                            if (!isChecked) {
                                Toast.makeText(context, context.getString(R.string.sync_system_calendar_toast_cleared), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }

        // Section 3: 系统保障与数据 / Advanced & System Infrastructure
        item {
            SectionHeader(title = stringResource(R.string.settings_section_system_infra))
        }

        item {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    iconColor = AccentOrange,
                    title = stringResource(R.string.llm_settings),
                    subtitle = if (state.aiChannelMode == SettingsRepository.MODE_OFFICIAL) {
                        stringResource(R.string.llm_settings_sub_official)
                    } else {
                        if (state.apiKey.isBlank()) {
                            stringResource(R.string.llm_settings_sub_custom_unconfigured)
                        } else {
                            stringResource(R.string.llm_settings_sub_custom_configured, state.model)
                        }
                    },
                    onClick = onNavigateToLlmSettings
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    iconColor = accentColor,
                    title = stringResource(R.string.lockscreen_cloud_title),
                    subtitle = stringResource(R.string.lockscreen_cloud_sub),
                    onClick = { viewModel.openLockscreenDialog() }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Build,
                    iconColor = Color(0xFF2EC4B6),
                    title = stringResource(R.string.survival_guide),
                    subtitle = stringResource(R.string.survival_guide_sub),
                    onClick = onNavigateToSurvivalGuide
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Share,
                    iconColor = Color(0xFF3F51B5),
                    title = stringResource(R.string.export_backup_title),
                    subtitle = stringResource(R.string.export_backup_sub),
                    onClick = {
                        viewModel.exportBackup { jsonStr ->
                            pendingExportJson = jsonStr
                            showExportOptionsDialog = true
                        }
                    }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Send,
                    iconColor = Color(0xFF00897B),
                    title = stringResource(R.string.import_backup_title),
                    subtitle = stringResource(R.string.import_backup_sub),
                    onClick = {
                        jsonPickerLauncher?.launch("application/json")
                            ?: Toast.makeText(context, context.getString(R.string.storage_unavailable), Toast.LENGTH_SHORT).show()
                    }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Delete,
                    iconColor = Color.Red,
                    title = stringResource(R.string.clear_all_data),
                    subtitle = stringResource(R.string.clear_all_data_sub),
                    onClick = { viewModel.openClearConfirmDialog() }
                )
            }
        }

        // Section 4: 帮助与关于 / Help & About
        item {
            SectionHeader(title = stringResource(R.string.settings_section_help_about))
        }

        item {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Info,
                    iconColor = AccentOrange,
                    title = stringResource(R.string.user_guide_title),
                    subtitle = stringResource(R.string.user_guide_sub),
                    onClick = onNavigateToUserGuide
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Info,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = stringResource(R.string.about_timeless),
                    subtitle = stringResource(R.string.about_timeless_sub),
                    onClick = onNavigateToAbout
                )
            }
        }
    }



    // Lockscreen & Fluid Cloud Diagnostic Dialog
    if (state.showLockscreenDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeLockscreenDialog() },
            title = { Text(stringResource(R.string.dialog_lockscreen_title), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.dialog_lockscreen_content),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.closeLockscreenDialog()
                        val channelIntent = Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, ClassBellApp.CHANNEL_IN_CLASS_ID)
                        }
                        runCatching {
                            context.startActivity(channelIntent)
                        }.onFailure {
                            val appIntent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            runCatching {
                                context.startActivity(appIntent)
                            }.onFailure {
                                val fallback = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(fallback)
                            }
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text(stringResource(R.string.btn_open_system_notification))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeLockscreenDialog() }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }



    // Export Backup Dialog (Local Save vs App Share)
    if (showExportOptionsDialog && pendingExportJson != null) {
        val currentJson = pendingExportJson ?: ""
        AlertDialog(
            onDismissRequest = {
                showExportOptionsDialog = false
                pendingExportJson = null
            },
            title = {
                Text(
                    text = stringResource(R.string.export_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.export_dialog_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 1: Save directly to phone storage (SAF)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable {
                                showExportOptionsDialog = false
                                val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                                val defaultName = "Timeless_Backup_$timeStamp.json"
                                createDocumentLauncher?.launch(defaultName)
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AccentOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = AccentOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.export_save_local_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.export_save_local_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Option 2: Share to external apps (QQ, WeChat, Email)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable {
                                showExportOptionsDialog = false
                                shareBackupFile(currentJson)
                                pendingExportJson = null
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3F51B5).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color(0xFF3F51B5),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.export_share_apps_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.export_share_apps_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showExportOptionsDialog = false
                        pendingExportJson = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }



    // Clear Data Confirmation Dialog
    if (state.showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeClearConfirmDialog() },
            title = { Text(stringResource(R.string.dialog_clear_title)) },
            text = { Text(stringResource(R.string.dialog_clear_content)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData {
                        Toast.makeText(context, context.getString(R.string.data_reset_success), Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(stringResource(R.string.confirm), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeClearConfirmDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Warn Minutes Slider Dialog
    if (showWarnMinutesDialog) {
        var sliderValue by remember { mutableFloatStateOf(state.endWarnMinutes.toFloat()) }
        AlertDialog(
            onDismissRequest = { showWarnMinutesDialog = false },
            title = { Text(stringResource(R.string.dialog_warn_minutes_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.dialog_warn_minutes_content, sliderValue.toInt()))
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 1f..15f,
                        steps = 13
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateWarnMinutes(sliderValue.toInt())
                    showWarnMinutesDialog = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarnMinutesDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }


}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = AccentOrange,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsItemWithSwitch(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iconColor
            )
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
private fun RoleSelectorCard(
    currentRole: String,
    onRoleSelected: (String) -> Unit,
    accentColor: Color
) {
    val isTeacher = currentRole != SettingsRepository.ROLE_STUDENT

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (!isTeacher) Icons.Default.School else Icons.Default.CoPresent,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.role_mode_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!isTeacher) {
                            stringResource(R.string.role_student_desc)
                        } else {
                            stringResource(R.string.role_teacher_desc)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Smooth Physical Sliding Capsule Switcher
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(3.dp)
            ) {
                val tabWidth = maxWidth / 2
                val targetOffset = if (isTeacher) 0.dp else tabWidth
                val animatedOffset by animateDpAsState(
                    targetValue = targetOffset,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 450f),
                    label = "role_sliding_pill"
                )

                // Sliding active pill
                Box(
                    modifier = Modifier
                        .offset(x = animatedOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .shadow(elevation = 3.dp, shape = RoundedCornerShape(10.dp))
                        .background(accentColor, shape = RoundedCornerShape(10.dp))
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onRoleSelected(SettingsRepository.ROLE_TEACHER) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CoPresent,
                                contentDescription = null,
                                tint = if (isTeacher) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.role_teacher),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isTeacher) FontWeight.Bold else FontWeight.Normal,
                                color = if (isTeacher) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onRoleSelected(SettingsRepository.ROLE_STUDENT) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = if (!isTeacher) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.role_student),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (!isTeacher) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isTeacher) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Dynamic sliding preview card with themed gradient background
            AnimatedContent(
                targetState = isTeacher,
                transitionSpec = {
                    if (targetState) {
                        // Switching to Teacher (slide in from left)
                        (slideInHorizontally(animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f)) { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally(animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f)) { it } + fadeOut())
                    } else {
                        // Switching to Student (slide in from right)
                        (slideInHorizontally(animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f)) { it } + fadeIn()) togetherWith
                        (slideOutHorizontally(animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f)) { -it } + fadeOut())
                    }
                },
                label = "role_focus_preview"
            ) { teacherModeActive ->
                val bgBrush = if (teacherModeActive) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.16f),
                            accentColor.copy(alpha = 0.04f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF4361EE).copy(alpha = 0.16f),
                            accentColor.copy(alpha = 0.05f)
                        )
                    )
                }
                val borderColor = if (teacherModeActive) {
                    accentColor.copy(alpha = 0.25f)
                } else {
                    Color(0xFF4361EE).copy(alpha = 0.25f)
                }
                val iconTint = if (teacherModeActive) accentColor else Color(0xFF4361EE)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(bgBrush)
                        .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (teacherModeActive) {
                                stringResource(R.string.role_preview_teacher_title)
                            } else {
                                stringResource(R.string.role_preview_student_title)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = iconTint
                        )
                        Text(
                            text = if (teacherModeActive) {
                                stringResource(R.string.role_preview_teacher_concise)
                            } else {
                                stringResource(R.string.role_preview_student_concise)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

