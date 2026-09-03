package com.classbell.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.classbell.app.ui.theme.PinkPrimary

@Composable
fun LlmSettingsDialog(
    initialApiKey: String,
    initialBaseUrl: String,
    initialModel: String,
    isTesting: Boolean,
    testMessage: String?,
    onDismiss: () -> Unit,
    onSave: (apiKey: String, baseUrl: String, model: String) -> Unit,
    onTest: (apiKey: String, baseUrl: String, model: String) -> Unit
) {
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var baseUrl by remember { mutableStateOf(initialBaseUrl.ifEmpty { "https://open.bigmodel.cn/api/paas/v4" }) }
    var model by remember { mutableStateOf(initialModel.ifEmpty { "glm-4.5v" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "大模型 API 设置 (BYOK)",
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
                Text(
                    text = "支持任意兼容 OpenAI 接口规范的多模态大模型服务（如智谱 GLM-4.5v、阿里 Qwen-VL、OpenAI GPT-4o 等）。密钥保存在本机 DataStore，绝不上报。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key *") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Test Connection Button & Message
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onTest(apiKey, baseUrl, model) },
                        enabled = !isTesting && apiKey.isNotBlank()
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("测试连接")
                    }

                    testMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (it.contains("成功")) PinkPrimary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(apiKey, baseUrl, model) },
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
