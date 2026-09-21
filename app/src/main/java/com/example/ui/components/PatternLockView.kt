package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassAccentCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun PatternLockView(
    onPatternComplete: (List<Int>) -> Unit,
    isError: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val selectedPoints = remember { mutableStateListOf<Int>() }
    var currentTouchPos by remember { mutableStateOf<Offset?>(null) }
    val scope = rememberCoroutineScope()

    // 3x3 node positions and hit radius
    var nodePositions by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var calculatedHitRadius by remember { mutableFloatStateOf(100f) }

    // Error shake animation
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(isError) {
        if (isError) {
            // Rapid horizontal shake animation
            repeat(3) {
                shakeOffset.animateTo(22f, tween(40))
                shakeOffset.animateTo(-22f, tween(40))
            }
            shakeOffset.animateTo(0f, spring())
            delay(400)
            selectedPoints.clear()
            currentTouchPos = null
        }
    }

    val patternColor = when {
        isError -> NeonRed
        else -> GlassAccentCyan
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PatternStrengthIndicator(selectedCount = selectedPoints.size, isError = isError)

        Box(
            modifier = Modifier
                .size(320.dp)
                .padding(16.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedPoints.clear()
                            currentTouchPos = offset
                            checkHit(offset, nodePositions, calculatedHitRadius)?.let { pointIndex ->
                                if (!selectedPoints.contains(pointIndex)) {
                                    selectedPoints.add(pointIndex)
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val pos = change.position
                            currentTouchPos = pos
                            checkHit(pos, nodePositions, calculatedHitRadius)?.let { pointIndex ->
                                if (!selectedPoints.contains(pointIndex)) {
                                    selectedPoints.add(pointIndex)
                                }
                            }
                        },
                        onDragEnd = {
                            currentTouchPos = null
                            if (selectedPoints.isNotEmpty()) {
                                onPatternComplete(selectedPoints.toList())
                            }
                        },
                        onDragCancel = {
                            currentTouchPos = null
                            selectedPoints.clear()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sizePx = size.minDimension
                val step = sizePx / 3f
                val radius = step * 0.16f
                val hitRadius = step * 0.44f // Generous density-aware hit threshold
                calculatedHitRadius = hitRadius

                val positions = mutableListOf<Offset>()
                for (row in 0..2) {
                    for (col in 0..2) {
                        val cx = col * step + step / 2f
                        val cy = row * step + step / 2f
                        positions.add(Offset(cx, cy))
                    }
                }
                nodePositions = positions

                // Draw connecting lines
                if (selectedPoints.size > 1) {
                    for (i in 0 until selectedPoints.size - 1) {
                        val start = positions[selectedPoints[i]]
                        val end = positions[selectedPoints[i + 1]]
                        drawLine(
                            brush = Brush.linearGradient(
                                listOf(patternColor.copy(alpha = 0.85f), patternColor)
                            ),
                            start = start,
                            end = end,
                            strokeWidth = 9.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Draw trailing line to current drag touch pos
                val lastPoint = selectedPoints.lastOrNull()
                if (lastPoint != null && currentTouchPos != null) {
                    val start = positions[lastPoint]
                    drawLine(
                        color = patternColor.copy(alpha = 0.6f),
                        start = start,
                        end = currentTouchPos!!,
                        strokeWidth = 7.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Draw 3x3 nodes with animated halos
                positions.forEachIndexed { index, center ->
                    val isSelected = selectedPoints.contains(index)
                    val outerGlowRadius = if (isSelected) radius * 2.3f else radius * 1.4f

                    // Outer halo
                    if (isSelected) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(patternColor.copy(alpha = 0.5f), Color.Transparent),
                                center = center,
                                radius = outerGlowRadius
                            ),
                            radius = outerGlowRadius,
                            center = center
                        )
                        drawCircle(
                            color = patternColor.copy(alpha = 0.8f),
                            radius = radius * 1.6f,
                            center = center,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    } else {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.22f),
                            radius = radius * 1.25f,
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Center node dot
                    drawCircle(
                        color = if (isSelected) patternColor else Color.White,
                        radius = if (isSelected) radius * 0.85f else radius * 0.45f,
                        center = center
                    )
                }
            }
        }
    }
}

private fun checkHit(pos: Offset, positions: List<Offset>, hitThreshold: Float): Int? {
    positions.forEachIndexed { index, offset ->
        val dist = hypot((pos.x - offset.x).toDouble(), (pos.y - offset.y).toDouble()).toFloat()
        if (dist <= hitThreshold) return index
    }
    return null
}

@Composable
fun PatternStrengthIndicator(selectedCount: Int, isError: Boolean = false) {
    val (label, color) = when {
        isError -> "Incorrect pattern. Try again." to NeonRed
        selectedCount == 0 -> "Connect at least 4 dots" to Color.White.copy(alpha = 0.85f)
        selectedCount < 4 -> "Too short (connect at least 4 dots)" to Color(0xFFFF9900)
        selectedCount in 4..5 -> "Good Pattern" to GlassAccentCyan
        else -> "Strong Pattern" to NeonGreen
    }

    Row(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
