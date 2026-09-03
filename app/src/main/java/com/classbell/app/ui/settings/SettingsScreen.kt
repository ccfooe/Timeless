package com.classbell.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classbell.app.data.backup.ImportMode
import com.classbell.app.ui.import.ImportViewModel
import com.classbell.app.ui.theme.PinkDark
import com.classbell.app.ui.theme.PinkPrimary

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

    var showWarnMinutesDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "我的与设置",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "教师专属助手 · 数据全本地存储",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 1: AI 智能截图导入
        item {
            SectionHeader(title = "AI 智能截图导入")
        }

        item {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.CameraAlt,
                    iconColor = PinkPrimary,
                    title = "导入课程表截图 (LLM)",
                    subtitle = "选择教务系统课表截图，AI 自动提取课程、班级、教室",
                    onClick = { timetablePickerLauncher.launch("image/*") }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.CloudUpload,
                    iconColor = Color(0xFF7209B7),
                    title = "导入校历截图 (LLM)",
                    subtitle = "提取第一周开学日期、教学周与节假日安排",
                    onClick = { calendarPickerLauncher.launch("image/*") }
                )
            }
        }

        // Section 2: 课程与提醒设置
        item {
            SectionHeader(title = "课表与提醒")
        }

        item {
            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Schedule,
                    iconColor = Color(0xFF4361EE),
                    title = "作息时间表设置",
                    subtitle = "当前为10节课作息（含晚课），可自定义各节起止时间",
                    onClick = onNavigateToPeriodEditor
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Alarm,
                    iconColor = Color(0xFFFF9F1C),
                    title = "下课前提醒时间",
                    subtitle = "提前 ${state.endWarnMinutes} 分钟发送震动与横幅提醒",
                    onClick = { showWarnMinutesDialog = true }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.BatteryChargingFull,
                    iconColor = Color(0xFF2EC4B6),
                    title = "ColorOS 后台保活与提醒指南",
                    subtitle = "电池优化、自启动与多任务后台锁定设置",
                    onClick = { viewModel.openSurvivalGuide() }
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
                    iconColor = PinkDark,
                    title = "大模型 API 设置 (BYOK)",
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
                    icon = Icons.Default.DeleteForever,
                    iconColor = Color.Red,
                    title = "清除全部数据",
                    subtitle = "重置数据库至初始状态",
                    onClick = { viewModel.openClearConfirm() }
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
                    icon = Icons.Default.HelpOutline,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = "关于 ClassBell",
                    subtitle = "v1.0.0 (Build 1) · 适用高校教师",
                    onClick = { showAboutDialog = true }
                )
            }
        }
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

    // Clear Data Confirmation Dialog
    if (state.showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeClearConfirm() },
            title = { Text("确定清除全部数据？", fontWeight = FontWeight.Bold) },
            text = { Text("此操作将清空本地所有学期、课程、作息表与事务数据，无法撤销。建议您先进行数据导出备份。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        Toast.makeText(context, "数据已清除", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("确认清空", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeClearConfirm() }) {
                    Text("取消")
                }
            }
        )
    }

    // Warn Minutes Config Dialog
    if (showWarnMinutesDialog) {
        var sliderVal by remember { mutableFloatStateOf(state.endWarnMinutes.toFloat()) }
        AlertDialog(
            onDismissRequest = { showWarnMinutesDialog = false },
            title = { Text("下课前提醒时间", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("在课程结束前多少分钟发送震动与横幅提醒：")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "提前 ${sliderVal.toInt()} 分钟",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PinkPrimary
                    )
                    Slider(
                        value = sliderVal,
                        onValueChange = { sliderVal = it },
                        valueRange = 1f..15f,
                        steps = 13
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateWarnMinutes(sliderVal.toInt())
                        showWarnMinutesDialog = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarnMinutesDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("关于 ClassBell", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ClassBell 是专为高校授课教师量身定制的课表与下课提醒伴侣应用。")
                    Text("• 智能导入：支持通过多模态大模型从教务系统课程表与校历截图中一键提取数据。")
                    Text("• 课堂专注：锁屏常驻网易云音乐风格倒计时进度条，下课前准时震动提醒。")
                    Text("• 隐私安全：100% 离线优先，不设服务器，自备 API Key，本地数据支持随时导出。")
                    Text("版本: 1.0.0 (Release)")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsItem(
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
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
fun SettingsDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )
}
