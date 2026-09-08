package com.classbell.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
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

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    importViewModel: ImportViewModel,
    onNavigateToPeriodEditor: () -> Unit,
    onNavigateToImportConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Image Picker for Timetable Screenshot
    val timetablePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            importViewModel.parseTimetableImage(it)
            onNavigateToImportConfirm()
        }
    }

    // Image Picker for Calendar Screenshot
    val calendarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            importViewModel.parseCalendarImage(it)
            onNavigateToImportConfirm()
        }
    }

    // File Picker for JSON Backup Import
    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                    val jsonContent = reader.readText()
                    viewModel.importBackup(jsonContent) { success ->
                        val msg = if (success) "备份恢复成功" else "备份文件解析失败"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }.onFailure {
                Toast.makeText(context, "读取文件失败: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showWarnMinutesDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

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

        // Section 0: 通用 / General (Language Switcher)
        item {
            SectionHeader(title = "通用设置 / General")
        }

        item {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Language,
                    iconColor = AccentOrange,
                    title = stringResource(R.string.language_setting),
                    subtitle = if (state.appLanguage == "en") "English" else "简体中文",
                    onClick = { viewModel.openLanguageDialog() }
                )
            }
        }

        // Section 1: 智能识别导入 (LLM)
        item {
            SectionHeader(title = "智能识别录入 (LLM)")
        }

        item {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.CameraAlt,
                    iconColor = AccentOrange,
                    title = stringResource(R.string.import_schedule_screenshot),
                    subtitle = "拍照或相册选取作息课程表图片，AI 自动提取",
                    onClick = { timetablePickerLauncher.launch("image/*") }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.CloudUpload,
                    iconColor = Color(0xFF7209B7),
                    title = stringResource(R.string.import_calendar_screenshot),
                    subtitle = "提取第一周开学日期、教学周与节假日安排",
                    onClick = { calendarPickerLauncher.launch("image/*") }
                )
            }
        }

        // Section 2: 课表与提醒设置
        item {
            SectionHeader(title = "课表与提醒")
        }

        item {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Schedule,
                    iconColor = Color(0xFF4361EE),
                    title = stringResource(R.string.period_table_manager),
                    subtitle = "当前为10节课作息（含晚课），可自定义各节起止时间",
                    onClick = onNavigateToPeriodEditor
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Alarm,
                    iconColor = AccentAmber,
                    title = "下课前提醒时间",
                    subtitle = "提前 ${state.endWarnMinutes} 分钟发送震动与横幅提醒",
                    onClick = { showWarnMinutesDialog = true }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.BatteryChargingFull,
                    iconColor = Color(0xFF2EC4B6),
                    title = stringResource(R.string.survival_guide),
                    subtitle = "电池优化、自启动与多任务后台锁定设置",
                    onClick = { viewModel.openSurvivalDialog() }
                )
            }
        }

        // Section 3: API 与数据管理
        item {
            SectionHeader(title = "API 与备份")
        }

        item {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Key,
                    iconColor = AccentOrange,
                    title = stringResource(R.string.llm_settings),
                    subtitle = if (state.apiKey.isBlank()) "未配置密钥 (点击添加)" else "已配置: ${state.model}",
                    onClick = { viewModel.openLlmDialog() }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Share,
                    iconColor = Color(0xFF3F51B5),
                    title = "导出本地数据备份 (JSON)",
                    subtitle = "包含学期、课程、作息与事务（不含 API Key）",
                    onClick = {
                        viewModel.exportBackup { jsonStr ->
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, jsonStr)
                                type = "application/json"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "分享备份文件"))
                        }
                    }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.UploadFile,
                    iconColor = Color(0xFF00897B),
                    title = "导入数据备份 (JSON)",
                    subtitle = "从 JSON 文件中恢复全部课程、学期与日程",
                    onClick = { jsonPickerLauncher.launch("application/json") }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    iconColor = Color.Red,
                    title = stringResource(R.string.clear_all_data),
                    subtitle = "重置数据库至初始状态",
                    onClick = { viewModel.openClearConfirmDialog() }
                )
            }
        }

        // Section 4: 关于
        item {
            SectionHeader(title = "关于")
        }

        item {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Language,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = stringResource(R.string.about_tempo),
                    subtitle = "v2.0.0 · 专为高校教师打造的课堂节拍掌控伴侣",
                    onClick = { showAboutDialog = true }
                )
            }
        }
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
            onDismiss = { viewModel.closeSurvivalDialog() }
        )
    }

    // Clear Data Confirmation Dialog
    if (state.showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeClearConfirmDialog() },
            title = { Text("重置本地数据") },
            text = { Text("确定要清除所有课程、校历与日程数据吗？此操作无法撤销。清除后将恢复内置的初始学期与作息时间表。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData {
                        Toast.makeText(context, "数据已重置", Toast.LENGTH_SHORT).show()
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
            title = { Text("设置下课前提醒") },
            text = {
                Column {
                    Text("在下课前 ${sliderValue.toInt()} 分钟通过强力震动与锁屏横幅提醒防拖堂。")
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
            title = { Text(stringResource(R.string.about_tempo), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tempo 律动",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                    Text(
                        text = "版本：v2.0.0 Pro\n" +
                                "核心特性：\n" +
                                "• 苹果级毛玻璃灵动倒计时与防拖堂震动\n" +
                                "• 锁屏常驻进度条，息屏掌控课堂节奏\n" +
                                "• ColorOS 16 风格教学日历与 10 节作息网格\n" +
                                "• 多模态 AI 大模型 (BYOK) 课表校历拍照录入\n" +
                                "• 离线本地运行，绝对保护教学隐私",
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
            imageVector = Icons.Default.ChevronRight,
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
