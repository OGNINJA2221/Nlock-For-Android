package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassAccentCyan
import com.example.ui.theme.GlassWhiteBorder
import com.example.ui.theme.GlassWhiteLow
import com.example.ui.theme.GlassWhiteMedium

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = GlassWhiteMedium,
    borderColor: Color = GlassWhiteBorder,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = modifier
            .shadow(elevation, shape, clip = false)
            .border(
                BorderStroke(
                    borderWidth,
                    Brush.verticalGradient(
                        colors = listOf(borderColor, borderColor.copy(alpha = 0.15f))
                    )
                ),
                shape
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Box(
            modifier = Modifier
                .padding(contentPadding)
        ) {
            content()
        }
    }
}

@Composable
fun GlassCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 76.dp,
    borderWidth: Dp = 1.5.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "scale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.75f else 0.35f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "glow"
    )

    Box(
        modifier = modifier
            .scale(scale)
            // Outer blue glow effect ring
            .border(
                BorderStroke(
                    2.dp,
                    Brush.radialGradient(
                        colors = listOf(
                            GlassAccentCyan.copy(alpha = glowAlpha),
                            Color.Transparent
                        )
                    )
                ),
                CircleShape
            )
            .clip(CircleShape)
            // Frosted glass appearance with semi-transparent white fill
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.36f else 0.24f),
                        Color.White.copy(alpha = if (isPressed) 0.20f else 0.12f)
                    )
                )
            )
            // Crisp white border
            .border(
                BorderStroke(
                    borderWidth,
                    if (isPressed) GlassAccentCyan else Color.White.copy(alpha = 0.50f)
                ),
                CircleShape
            )
            // Ripple animation
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = GlassAccentCyan),
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
