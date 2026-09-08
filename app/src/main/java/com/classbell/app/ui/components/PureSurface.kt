package com.classbell.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Pure Surface Color Palette: Apple / Smartisan Clean High-Order Surfaces
private val SurfaceLight = Color(0xFFFFFFFF)
private val SurfaceBorderLight = Color(0x0F000000) // 6% black hairline

private val SurfaceDark = Color(0xFF161619)
private val SurfaceBorderDark = Color(0x1AFFFFFF) // 10% white hairline

/**
 * PureSurface: A distraction-free, Apple/Smartisan inspired container.
 * Replaces noisy glassmorphism with crisp surface planes, micro hairline borders,
 * and optional spring-damping physical press reactions.
 */
@Composable
fun PureSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    contentPadding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    enableSpringTouch: Boolean = onClick != null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) SurfaceDark else SurfaceLight
    val borderColor = if (isDark) SurfaceBorderDark else SurfaceBorderLight

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enableSpringTouch) 0.982f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "surface_press_spring"
    )

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Surface(
        modifier = modifier
            .scale(scale)
            .then(clickableModifier),
        shape = shape,
        color = bgColor,
        border = BorderStroke(0.6.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = if (isDark) 0.dp else 1.dp
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
