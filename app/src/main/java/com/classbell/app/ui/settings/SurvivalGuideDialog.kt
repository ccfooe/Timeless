package com.classbell.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classbell.app.ui.theme.PinkPrimary

@Composable
fun SurvivalGuideDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ColorOS / Android 后台保活指南",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "为确保下课倒计时常驻卡片与提前5分钟提醒在手机锁屏或灭屏时准时触发，请完成以下设置：",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                GuideStep(
                    step = "1",
                    title = "允许通知、锁屏显示与 ColorOS 流体云",
                    desc = "点击下方按钮进入通知设置，务必开启「允许通知」、「锁屏通知（显示全部内容）」及「流体云」实时胶囊开关。"
                )

                GuideStep(
                    step = "2",
                    title = "忽略电池优化 (无限制)",
                    desc = "点击下方按钮，将本应用电池策略设置为「无限制/不优化」，避免系统休眠时冻结倒计时与闹钟。"
                )

                GuideStep(
                    step = "3",
                    title = "ColorOS 允许自启动与关联启动",
                    desc = "进入系统设置 → 应用管理 → 自启动管理，将 Timeless 设为「允许自启动」。"
                )

                GuideStep(
                    step = "4",
                    title = "多任务后台锁定",
                    desc = "上划进入多任务卡片页面，点击 Timeless 卡片右上角菜单，选择「锁定」，防止一键清理误杀。"
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        openNotificationSettings(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("前往设置：通知、锁屏与流体云权限")
                }

                OutlinedButton(
                    onClick = {
                        requestBatteryOptimizationExemption(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("前往设置：忽略电池优化")
                }

                OutlinedButton(
                    onClick = {
                        openAppSettings(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("前往设置：应用权限详情")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
            ) {
                Text("我已了解并配置")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("暂不配置")
            }
        }
    )
}

@Composable
private fun GuideStep(step: String, title: String, desc: String) {
    Column {
        Text(
            text = "$step. $title",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PinkPrimary
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun requestBatteryOptimizationExemption(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    context.startActivity(intent)
}
