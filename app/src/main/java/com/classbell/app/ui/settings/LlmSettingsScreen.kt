package com.classbell.app.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.classbell.app.ui.components.ClassBellSecondaryTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classbell.app.R
import com.classbell.app.data.repository.SettingsRepository
import com.classbell.app.ui.components.PureSurface
import com.classbell.app.ui.theme.LocalAccentColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current

    var selectedMode by remember(state.aiChannelMode) {
        mutableStateOf(if (state.aiChannelMode.isBlank()) SettingsRepository.MODE_OFFICIAL else state.aiChannelMode)
    }

    var officialModel by remember(state.officialModel) {
        mutableStateOf(if (state.officialModel.isBlank()) SettingsRepository.DEFAULT_OFFICIAL_MODEL else state.officialModel)
    }

    val sanitizedInitialEndpoint = when {
        state.officialEndpoint.isBlank() || state.officialEndpoint.contains("workers.dev") || state.officialEndpoint.contains("generativelanguage") -> SettingsRepository.DEFAULT_OFFICIAL_ENDPOINT
        else -> state.officialEndpoint.trim()
    }
    var officialEndpoint by remember(state.officialEndpoint, officialModel) { mutableStateOf(sanitizedInitialEndpoint) }
    var showAdvancedOfficial by remember { mutableStateOf(false) }

    var apiKey by remember(state.apiKey) { mutableStateOf(state.apiKey) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var baseUrl by remember(state.baseUrl) { mutableStateOf(state.baseUrl.ifEmpty { "https://open.bigmodel.cn/api/paas/v4" }) }
    var model by remember(state.model) { mutableStateOf(state.model.ifEmpty { "glm-4.6v" }) }

    val isOfficial = selectedMode == SettingsRepository.MODE_OFFICIAL

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ClassBellSecondaryTopBar(
                title = stringResource(R.string.llm_dialog_title),
                onNavigateBack = onNavigateBack
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Segmented Mode Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LlmCategoryChip(
                    icon = Icons.Default.Cloud,
                    label = stringResource(R.string.ai_channel_tab_official),
                    isSelected = isOfficial,
                    accentColor = accentColor,
                    onClick = { selectedMode = SettingsRepository.MODE_OFFICIAL },
                    modifier = Modifier.weight(1f)
                )
                LlmCategoryChip(
                    icon = Icons.Default.Settings,
                    label = stringResource(R.string.ai_channel_tab_custom),
                    isSelected = !isOfficial,
                    accentColor = accentColor,
                    onClick = { selectedMode = SettingsRepository.MODE_CUSTOM },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isOfficial) {
                    // Introduction & Status Card
                    item {
                        PureSurface(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = 18.dp
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "内置官方免密通道",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = stringResource(R.string.ai_channel_official_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    // Official Unified Channel: Timeless 官方智能云通道
                    item {
                        OfficialModelSelectCard(
                            title = stringResource(R.string.official_model_glm_title),
                            badge = stringResource(R.string.official_model_glm_badge),
                            description = stringResource(R.string.official_model_glm_desc),
                            isSelected = true,
                            badgeColor = Color(0xFF34C759),
                            accentColor = accentColor,
                            onClick = {
                                officialModel = SettingsRepository.DEFAULT_OFFICIAL_MODEL
                                officialEndpoint = SettingsRepository.DEFAULT_OFFICIAL_ENDPOINT
                            }
                        )
                    }

                    // Relay Node Endpoint & Advanced Config
                    item {
                        PureSurface(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = 18.dp
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.llm_official_node_label),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = officialEndpoint,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    TextButton(onClick = { showAdvancedOfficial = !showAdvancedOfficial }) {
                                        Text(
                                            text = if (showAdvancedOfficial) "收起节点" else "自定义节点",
                                            fontSize = 12.sp,
                                            color = accentColor
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = showAdvancedOfficial) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = officialEndpoint,
                                            onValueChange = { officialEndpoint = it },
                                            label = { Text("官方接口 Endpoint") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = {
                                                officialEndpoint = SettingsRepository.DEFAULT_OFFICIAL_ENDPOINT
                                            }) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(stringResource(R.string.llm_official_reset_node))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Custom API Key Card
                    item {
                        PureSurface(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = 18.dp
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = stringResource(R.string.ai_channel_custom_intro),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )

                                // Quick preset selector chips
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "快速预设（点击自动填充 Base URL 与模型名）：",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LlmPresetChip(
                                            name = "智谱 GLM",
                                            isSelected = baseUrl.contains("bigmodel.cn"),
                                            accentColor = accentColor,
                                            onClick = {
                                                baseUrl = "https://open.bigmodel.cn/api/paas/v4"
                                                model = "glm-4.6v"
                                            }
                                        )
                                        LlmPresetChip(
                                            name = "OpenAI (GPT-4o)",
                                            isSelected = baseUrl.contains("api.openai.com"),
                                            accentColor = accentColor,
                                            onClick = {
                                                baseUrl = "https://api.openai.com/v1"
                                                model = "gpt-4o-mini"
                                            }
                                        )
                                        LlmPresetChip(
                                            name = "Google Gemini",
                                            isSelected = baseUrl.contains("generativelanguage.googleapis.com"),
                                            accentColor = accentColor,
                                            onClick = {
                                                baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai"
                                                model = "gemini-1.5-flash"
                                            }
                                        )
                                        LlmPresetChip(
                                            name = "OpenRouter",
                                            isSelected = baseUrl.contains("openrouter.ai"),
                                            accentColor = accentColor,
                                            onClick = {
                                                baseUrl = "https://openrouter.ai/api/v1"
                                                model = "google/gemini-flash-1.5"
                                            }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = apiKey,
                                    onValueChange = { apiKey = it },
                                    label = { Text("API Key") },
                                    placeholder = { Text("例如: sk-...") },
                                    singleLine = true,
                                    visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                            Icon(
                                                imageVector = if (apiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = baseUrl,
                                    onValueChange = { baseUrl = it },
                                    label = { Text("Base URL") },
                                    placeholder = { Text("https://open.bigmodel.cn/api/paas/v4") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = model,
                                    onValueChange = { model = it },
                                    label = { Text("Model Name") },
                                    placeholder = { Text("glm-4.6v / gpt-4o / qwen-vl-max") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text(
                                    text = "💡 提示：港澳台或海外高校校历因包含特殊纪年或历史节日易触发国内模型审核（1301），推荐使用上方的 OpenAI 或 Google Gemini 预设，无任何审查限制且识别准确率极佳。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Connection test message banner
                if (state.testConnectionMessage != null) {
                    val msg = state.testConnectionMessage ?: ""
                    val isSuccess = msg.startsWith("✓") || msg.contains("成功")
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSuccess) Color(0xFF34C759).copy(alpha = 0.12f)
                                    else Color(0xFFFF3B30).copy(alpha = 0.12f)
                                )
                                .padding(14.dp)
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSuccess) Color(0xFF248A3D) else Color(0xFFD32F2F)
                            )
                        }
                    }
                }

                // Action Buttons (Test Connection & Save)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.testLlmConnection(selectedMode, officialEndpoint, officialModel, apiKey, baseUrl, model)
                            },
                            enabled = !state.isTestingConnection,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.llm_testing))
                            } else {
                                Text(
                                    if (isOfficial) stringResource(R.string.llm_test_official)
                                    else stringResource(R.string.llm_test_custom)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.saveLlmSettings(selectedMode, officialEndpoint, officialModel, apiKey, baseUrl, model)
                                Toast.makeText(context, "设置已保存", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LlmCategoryChip(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LlmPresetChip(
    name: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OfficialModelSelectCard(
    title: String,
    badge: String,
    description: String,
    isSelected: Boolean,
    badgeColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    PureSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 26.dp)
            )
        }
    }
}
