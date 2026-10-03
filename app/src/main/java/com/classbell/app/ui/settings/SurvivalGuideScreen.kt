package com.classbell.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.classbell.app.ui.components.ClassBellSecondaryTopBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classbell.app.ClassBellApp
import com.classbell.app.R
import com.classbell.app.ui.components.PureSurface
import com.classbell.app.ui.theme.LocalAccentColor

@Composable
fun SurvivalGuideScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ClassBellSecondaryTopBar(
                title = stringResource(R.string.survival_title),
                onNavigateBack = onNavigateBack
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Intro Description Card
            item {
                PureSurface(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 18.dp
                ) {
                    Text(
                        text = stringResource(R.string.survival_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
            }

            // Step 1: Notifications & Fluid Cloud
            item {
                SurvivalStepCard(
                    stepNum = "1",
                    icon = Icons.Default.Notifications,
                    iconColor = accentColor,
                    title = stringResource(R.string.guide_step1_title),
                    desc = stringResource(R.string.guide_step1_desc),
                    actionLabel = stringResource(R.string.btn_goto_notifications),
                    onAction = { openNotificationSettings(context) }
                )
            }

            // Step 2: Battery Optimization
            item {
                SurvivalStepCard(
                    stepNum = "2",
                    icon = Icons.Default.BatteryAlert,
                    iconColor = Color(0xFFFF9500),
                    title = stringResource(R.string.guide_step2_title),
                    desc = stringResource(R.string.guide_step2_desc),
                    actionLabel = stringResource(R.string.btn_goto_battery),
                    onAction = { requestBatteryOptimizationExemption(context) }
                )
            }

            // Step 3: Auto-launch
            item {
                SurvivalStepCard(
                    stepNum = "3",
                    icon = Icons.Default.RocketLaunch,
                    iconColor = Color(0xFF34C759),
                    title = stringResource(R.string.guide_step3_title),
                    desc = stringResource(R.string.guide_step3_desc),
                    actionLabel = stringResource(R.string.btn_goto_permissions),
                    onAction = { openAppSettings(context) }
                )
            }

            // Step 4: Lock in Recents
            item {
                SurvivalStepCard(
                    stepNum = "4",
                    icon = Icons.Default.Lock,
                    iconColor = Color(0xFFAF52DE),
                    title = stringResource(R.string.guide_step4_title),
                    desc = stringResource(R.string.guide_step4_desc),
                    actionLabel = null,
                    onAction = null
                )
            }
        }
    }
}

@Composable
private fun SurvivalStepCard(
    stepNum: String,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    desc: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    PureSurface(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 18.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "$stepNum. $title",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(actionLabel)
                }
            }
        }
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
            runCatching { context.startActivity(intent) }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

private fun openNotificationSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, ClassBellApp.CHANNEL_IN_CLASS_ID)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(channelIntent)
            return
        }
        val appIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(appIntent)
            return
        }
    }
    val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(fallback) }
}
