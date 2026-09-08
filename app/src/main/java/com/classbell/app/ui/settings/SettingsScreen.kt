package com.classbell.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
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
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accentColor = LocalAccentColor.current
    val context = LocalContext.current
    val registryOwner = LocalActivityResultRegistryOwner.current

    // Image Picker for Timetable Screenshot (Safely guarded)
    val timetablePickerLauncher = if (registryOwner != null) {
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                importViewModel.parseTimetableImage(it)
                onNavigateToImportConfirm()
            }
        }
    } else null

    // Image Picker for Calendar Screenshot (Safely guarded)
    val calendarPickerLauncher = if (registryOwner != null) {
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                importViewModel.parseCalendarImage(it)
                onNavigateToImportConfirm()
            }
        }
    } else null

    // File Picker for JSON Backup Import (Safely guarded)
    val jsonPickerLauncher = if (registryOwner != null) {
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                runCatching {
                    context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                        val jsonContent = reader.readText()
                        viewModel.importBackup(jsonContent) { success ->
                            val msg = if (success) context.getString(R.string.backup_success) else context.getString(R.string.backup_failed)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }.onFailure {
                    Toast.makeText(context, context.getString(R.string.file_read_error, it.message ?: ""), Toast.LENGTH_SHORT).show()
                }
            }
        }
    } else null

    var showWarnMinutesDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showUserGuideDialog by remember { mutableStateOf(false) }

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

        // Section 0: 外观与个性化 / Appearance & Display
        item {
            SectionHeader(title = stringResource(R.string.appearance_settings))
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
                    onClick = { viewModel.openThemeModeDialog() }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Palette,
                    iconColor = accentColor,
                    title = stringResource(R.string.accent_color_title),
                    subtitle = stringResource(AppAccentColor.fromKey(state.accentColor).titleRes),
                    onClick = { viewModel.openAccentColorDialog() }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Settings,
                    iconColor = accentColor,
                    title = stringResource(R.string.language_setting),
                    subtitle = if (state.appLanguage == "en") "English" else "简体中文",
                    onClick = { viewModel.openLanguageDialog() }
                )
            }
        }

        // Section 1: 智能识别导入 (LLM)
        item {
            SectionHeader(title = stringResource(R.string.settings_section_import_ai))
        }

        item {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Edit,
                    iconColor = accentColor,
                    title = stringResource(R.string.import_schedule_screenshot),
                    subtitle = stringResource(R.string.import_schedule_screenshot_sub),
                    onClick = {
                        timetablePickerLauncher?.launch("image/*")
                            ?: Toast.makeText(context, context.getString(R.string.storage_unavailable), Toast.LENGTH_SHORT).show()
                    }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.DateRange,
                    iconColor = Color(0xFF7209B7),
                    title = stringResource(R.string.import_calendar_screenshot),
                    subtitle = stringResource(R.string.import_calendar_screenshot_sub),
                    onClick = {
                        calendarPickerLauncher?.launch("image/*")
                            ?: Toast.makeText(context, context.getString(R.string.storage_unavailable), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // Section 2: 课表与提醒设置
        item {
            SectionHeader(title = stringResource(R.string.settings_section_schedule))
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
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    iconColor = accentColor,
                    title = stringResource(R.string.warn_minutes_title),
                    subtitle = stringResource(R.string.warn_minutes_sub, state.endWarnMinutes),
                    onClick = { showWarnMinutesDialog = true }
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
                    onClick = { viewModel.openSurvivalGuide() }
                )
            }
        }

        // Section 3: 指南与帮助
        item {
            SectionHeader(title = stringResource(R.string.settings_section_guide))
        }

        item {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Info,
                    iconColor = AccentOrange,
                    title = stringResource(R.string.user_guide_title),
                    subtitle = stringResource(R.string.user_guide_sub),
                    onClick = { showUserGuideDialog = true }
                )
            }
        }

        // Section 4: API 与备份
        item {
            SectionHeader(title = stringResource(R.string.settings_section_data))
        }

        item {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    iconColor = AccentOrange,
                    title = stringResource(R.string.llm_settings),
                    subtitle = if (state.apiKey.isBlank()) stringResource(R.string.llm_settings_sub_unconfigured) else stringResource(R.string.llm_settings_sub_configured, state.model),
                    onClick = { viewModel.openLlmDialog() }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Share,
                    iconColor = Color(0xFF3F51B5),
                    title = stringResource(R.string.export_backup_title),
                    subtitle = stringResource(R.string.export_backup_sub),
                    onClick = {
                        viewModel.exportBackup { jsonStr ->
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, jsonStr)
                                type = "application/json"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_backup_chooser)))
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

        // Section 5: 关于
        item {
            SectionHeader(title = stringResource(R.string.settings_section_about))
        }

        item {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Info,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = stringResource(R.string.about_timeless),
                    subtitle = stringResource(R.string.about_timeless_sub),
                    onClick = { showAboutDialog = true }
                )
            }
        }
    }

    // Theme Mode Selection Dialog
    if (state.showThemeModeDialog) {
        val options = listOf(
            "system" to stringResource(R.string.theme_mode_system),
            "light" to stringResource(R.string.theme_mode_light),
            "dark" to stringResource(R.string.theme_mode_dark)
        )
        AlertDialog(
            onDismissRequest = { viewModel.closeThemeModeDialog() },
            title = { Text(stringResource(R.string.dialog_theme_mode_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setThemeMode(mode) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.closeThemeModeDialog() }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // Accent Color Selection Dialog
    if (state.showAccentColorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeAccentColorDialog() },
            title = { Text(stringResource(R.string.dialog_accent_color_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppAccentColor.entries.forEach { accent ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.setAccentColor(accent.key) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(accent.primary)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(accent.titleRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (state.accentColor == accent.key) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            RadioButton(
                                selected = state.accentColor == accent.key,
                                onClick = { viewModel.setAccentColor(accent.key) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.closeAccentColorDialog() }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
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
                        val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        runCatching {
                            context.startActivity(intent)
                        }.onFailure {
                            val fallback = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(fallback)
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

    // Language Selection Dialog
    if (state.showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeLanguageDialog() },
            title = { Text(stringResource(R.string.language_setting), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setAppLanguage("zh") }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = state.appLanguage == "zh",
                            onClick = { viewModel.setAppLanguage("zh") }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.language_zh), style = MaterialTheme.typography.bodyLarge)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setAppLanguage("en") }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = state.appLanguage == "en",
                            onClick = { viewModel.setAppLanguage("en") }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.language_en), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.closeLanguageDialog() }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // BYOK Settings Dialog
    if (state.showLlmDialog) {
        LlmSettingsDialog(
            initialApiKey = state.apiKey,
            initialBaseUrl = state.baseUrl,
            initialModel = state.model,
            isTesting = state.isTestingConnection,
            testMessage = state.testConnectionMessage,
            onDismiss = { viewModel.closeLlmDialog() },
            onSave = { key, url, mdl -> viewModel.saveLlmSettings(key, url, mdl) },
            onTest = { key, url, mdl -> viewModel.testLlmConnection(key, url, mdl) }
        )
    }

    // ColorOS Survival Guide Dialog
    if (state.showSurvivalDialog) {
        SurvivalGuideDialog(
            onDismiss = { viewModel.closeSurvivalGuide() }
        )
    }

    // Beginner User Guide Dialog
    if (showUserGuideDialog) {
        UserGuideDialog(
            onDismiss = { showUserGuideDialog = false }
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

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.about_timeless), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Timeless",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                    Text(
                        text = stringResource(R.string.dialog_about_body),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.close))
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
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    )
}
