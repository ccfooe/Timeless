package com.classbell.app.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classbell.app.R
import com.classbell.app.ui.components.PureSurface
import com.classbell.app.ui.theme.AppAccentColor
import com.classbell.app.ui.theme.LocalAccentColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel,
    initialTab: Int = 0,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accentColor = LocalAccentColor.current
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 2)) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ClassBellSecondaryTopBar(
                title = stringResource(R.string.appearance_screen_title),
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
            // Horizontal Segmented Category Navigation (Identical to User Guide)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppearanceCategoryChip(
                    icon = Icons.Default.Palette,
                    label = stringResource(R.string.tab_accent_color),
                    isSelected = selectedTab == 0,
                    accentColor = accentColor,
                    onClick = { selectedTab = 0 }
                )
                AppearanceCategoryChip(
                    icon = Icons.Default.DarkMode,
                    label = stringResource(R.string.tab_theme_mode),
                    isSelected = selectedTab == 1,
                    accentColor = accentColor,
                    onClick = { selectedTab = 1 }
                )
                AppearanceCategoryChip(
                    icon = Icons.Default.Translate,
                    label = stringResource(R.string.tab_language),
                    isSelected = selectedTab == 2,
                    accentColor = accentColor,
                    onClick = { selectedTab = 2 }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Animated Tab Content Area
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "appearance_tab_content",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (tab) {
                        0 -> {
                            // Section: Theme Accent Color
                            items(AppAccentColor.entries.size) { index ->
                                val accent = AppAccentColor.entries[index]
                                val isSelected = state.accentColor == accent.key
                                PureSurface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setAccentColor(accent.key) },
                                    contentPadding = 16.dp
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Luminous Color Swatch Disc
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .shadow(
                                                        elevation = if (isSelected) 4.dp else 1.dp,
                                                        shape = CircleShape
                                                    )
                                                    .clip(CircleShape)
                                                    .background(accent.primary)
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column {
                                                Text(
                                                    text = stringResource(accent.titleRes),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = accent.hexCode,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Selection indicator with ample breathing space
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = accent.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                        shape = CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Section: Theme Mode (System, Light, Dark)
                            val modeOptions = listOf(
                                Triple("system", R.string.theme_mode_system, R.string.theme_mode_system_desc),
                                Triple("light", R.string.theme_mode_light, R.string.theme_mode_light_desc),
                                Triple("dark", R.string.theme_mode_dark, R.string.theme_mode_dark_desc)
                            )
                            items(modeOptions.size) { index ->
                                val (modeKey, titleRes, descRes) = modeOptions[index]
                                val title = stringResource(titleRes)
                                val desc = stringResource(descRes)
                                val isSelected = state.themeMode == modeKey
                                val modeIcon = when (modeKey) {
                                    "light" -> Icons.Default.LightMode
                                    "dark" -> Icons.Default.DarkMode
                                    else -> Icons.Default.BrightnessAuto
                                }
                                PureSurface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setThemeMode(modeKey) },
                                    contentPadding = 16.dp
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(accentColor.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = modeIcon,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column {
                                                Text(
                                                    text = title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = desc,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                        shape = CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Section: Language (zh, en)
                            val langOptions = listOf(
                                Triple("zh", R.string.language_zh, R.string.language_zh_desc),
                                Triple("en", R.string.language_en, R.string.language_en_desc)
                            )
                            items(langOptions.size) { index ->
                                val (langKey, titleRes, descRes) = langOptions[index]
                                val title = stringResource(titleRes)
                                val desc = stringResource(descRes)
                                val isSelected = state.appLanguage == langKey
                                PureSurface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setAppLanguage(langKey) },
                                    contentPadding = 16.dp
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(accentColor.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Translate,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column {
                                                Text(
                                                    text = title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = desc,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                        shape = CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceCategoryChip(
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
