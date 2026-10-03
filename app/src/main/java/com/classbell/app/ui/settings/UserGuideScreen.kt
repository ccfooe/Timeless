package com.classbell.app.ui.settings

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.filled.CoPresent
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.classbell.app.ui.components.ClassBellSecondaryTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classbell.app.R
import com.classbell.app.ui.components.PureSurface
import com.classbell.app.ui.theme.LocalAccentColor

@Composable
fun UserGuideScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    var selectedCategory by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ClassBellSecondaryTopBar(
                title = stringResource(R.string.user_guide_header),
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
            // Horizontal Segmented Category Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GuideCategoryChip(
                    icon = Icons.Default.School,
                    label = stringResource(R.string.guide_tab_roles),
                    isSelected = selectedCategory == 0,
                    accentColor = accentColor,
                    onClick = { selectedCategory = 0 }
                )
                GuideCategoryChip(
                    icon = Icons.Default.AutoAwesome,
                    label = stringResource(R.string.guide_tab_scheduling),
                    isSelected = selectedCategory == 1,
                    accentColor = accentColor,
                    onClick = { selectedCategory = 1 }
                )
                GuideCategoryChip(
                    icon = Icons.Default.Schedule,
                    label = stringResource(R.string.guide_tab_clock),
                    isSelected = selectedCategory == 2,
                    accentColor = accentColor,
                    onClick = { selectedCategory = 2 }
                )
                GuideCategoryChip(
                    icon = Icons.Default.Security,
                    label = stringResource(R.string.guide_tab_privacy),
                    isSelected = selectedCategory == 3,
                    accentColor = accentColor,
                    onClick = { selectedCategory = 3 }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Animated Content Section
            AnimatedContent(
                targetState = selectedCategory,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "guide_tab_content",
                modifier = Modifier.fillMaxSize()
            ) { category ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (category) {
                        0 -> {
                            // Section: Role Modes
                            item {
                                GuideInfoCard(
                                    icon = Icons.Default.CoPresent,
                                    iconColor = accentColor,
                                    title = stringResource(R.string.guide_role_card_title),
                                    content = stringResource(R.string.guide_role_card_desc)
                                )
                            }
                        }
                        1 -> {
                            // Section: Scheduling Setup
                            item {
                                GuideInfoCard(
                                    icon = Icons.Default.AutoAwesome,
                                    iconColor = accentColor,
                                    title = stringResource(R.string.guide_schedule_ai_title),
                                    content = stringResource(R.string.guide_schedule_ai_desc)
                                )
                            }
                            item {
                                GuideInfoCard(
                                    icon = Icons.Default.Schedule,
                                    iconColor = Color(0xFF4361EE),
                                    title = stringResource(R.string.guide_schedule_manual_title),
                                    content = stringResource(R.string.guide_schedule_manual_desc)
                                )
                            }
                        }
                        2 -> {
                            // Section: In-Class & Lockscreen
                            item {
                                GuideInfoCard(
                                    icon = Icons.Default.Schedule,
                                    iconColor = accentColor,
                                    title = stringResource(R.string.guide_clock_card_title),
                                    content = stringResource(R.string.guide_clock_card_desc)
                                )
                            }
                        }
                        3 -> {
                            // Section: Offline Privacy & Backups
                            item {
                                GuideInfoCard(
                                    icon = Icons.Default.Lock,
                                    iconColor = Color(0xFF2EC4B6),
                                    title = stringResource(R.string.guide_privacy_card_title),
                                    content = stringResource(R.string.guide_privacy_card_desc)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideCategoryChip(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
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
private fun GuideInfoCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }
    }
}
