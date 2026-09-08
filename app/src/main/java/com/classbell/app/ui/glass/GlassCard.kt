package com.classbell.app.ui.glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.classbell.app.ui.theme.LocalIsDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.classbell.app.ui.theme.GlassDark
import com.classbell.app.ui.theme.GlassDarkStroke
import com.classbell.app.ui.theme.GlassWhite
import com.classbell.app.ui.theme.GlassWhiteStroke

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 3.dp,
    contentPadding: Dp = 18.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val bgColor = if (isDark) GlassDark else GlassWhite
    val strokeColor = if (isDark) GlassDarkStroke else GlassWhiteStroke

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        bgColor,
                        bgColor.copy(alpha = if (isDark) 0.92f else 0.88f)
                    )
                )
            )
    ) {
        Surface(
            modifier = Modifier.matchParentSize(),
            shape = shape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, strokeColor)
        ) {}

        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
