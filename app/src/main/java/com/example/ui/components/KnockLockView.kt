package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.KnockLayout
import com.example.ui.theme.GlassAccentCyan
import com.example.ui.theme.GlassPrimaryBlue
import com.example.ui.theme.GlassWhiteBorder
import com.example.ui.theme.GlassWhiteMedium
import com.example.ui.theme.NeonRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun KnockLockView(
    onKnockComplete: (List<Int>) -> Unit,
    sequenceTargetLength: Int = 4,
    layout: KnockLayout = KnockLayout.GRID_2X2_CENTER,
    isError: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentSequence = remember { mutableStateListOf<Int>() }
    val scope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(isError) {
        if (isError) {
            currentSequence.clear() // Immediately empty
            repeat(3) {
                shakeOffset.animateTo(20f, tween(40))
                shakeOffset.animateTo(-20f, tween(40))
            }
            shakeOffset.animateTo(0f, spring())
        }
    }

    fun triggerVibration() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onZoneTapped(zoneId: Int) {
        if (!enabled) return
        triggerVibration()
        currentSequence.add(zoneId)
        if (currentSequence.size >= sequenceTargetLength) {
            val completed = currentSequence.toList()
            onKnockComplete(completed)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Knock Progress Indicator
        val dotSpacing = if (sequenceTargetLength > 6) 10.dp else 14.dp
        val dotSize = if (sequenceTargetLength > 6) 14.dp else 16.dp

        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(dotSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until sequenceTargetLength) {
                val isKnocked = i < currentSequence.size
                val dotColor = when {
                    isError -> NeonRed
                    isKnocked -> GlassAccentCyan
                    else -> Color.White.copy(alpha = 0.25f)
                }
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(
                            1.5.dp,
                            if (isKnocked || isError) Color.White else GlassWhiteBorder,
                            CircleShape
                        )
                )
            }
        }

        val outerBorderColor = when {
            isError -> NeonRed
            else -> GlassAccentCyan.copy(alpha = 0.6f)
        }

        if (layout == KnockLayout.GRID_3X3) {
            // Mode 2: 3x3 Grid
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(GlassWhiteMedium)
                    .border(
                        BorderStroke(
                            2.dp,
                            Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.6f), outerBorderColor)
                            )
                        ),
                        RoundedCornerShape(32.dp)
                    )
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val gridLabels = listOf(
                        listOf(Triple(1, "TL", "TOP LEFT"), Triple(2, "TC", "TOP CTR"), Triple(3, "TR", "TOP RIGHT")),
                        listOf(Triple(4, "ML", "MID LEFT"), Triple(5, "CTR", "CENTER"), Triple(6, "MR", "MID RIGHT")),
                        listOf(Triple(7, "BL", "BTM LEFT"), Triple(8, "BC", "BTM CTR"), Triple(9, "BR", "BTM RIGHT"))
                    )

                    for (row in gridLabels) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (cell in row) {
                                KnockGrid3x3Cell(
                                    zoneId = cell.first,
                                    shortLabel = cell.second,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                                    onTap = { onZoneTapped(cell.first) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Mode 1: 2x2 Grid + Center
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(GlassWhiteMedium)
                    .border(
                        BorderStroke(
                            2.dp,
                            Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.6f), outerBorderColor)
                            )
                        ),
                        RoundedCornerShape(32.dp)
                    )
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        KnockQuadrant(
                            zoneId = 1,
                            label = "TOP LEFT",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            onTap = { onZoneTapped(1) }
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        KnockQuadrant(
                            zoneId = 2,
                            label = "TOP RIGHT",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            onTap = { onZoneTapped(2) }
                        )
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        KnockQuadrant(
                            zoneId = 3,
                            label = "BOTTOM LEFT",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            onTap = { onZoneTapped(3) }
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        KnockQuadrant(
                            zoneId = 4,
                            label = "BOTTOM RIGHT",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            onTap = { onZoneTapped(4) }
                        )
                    }
                }

                // Center Floating Knock Zone (Zone 5)
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .align(Alignment.Center)
                ) {
                    KnockCenterZone(
                        onTap = { onZoneTapped(5) }
                    )
                }
            }
        }

        // Reset Sequence button
        if (currentSequence.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { currentSequence.clear() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Clear Sequence",
                    color = GlassAccentCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun KnockGrid3x3Cell(
    zoneId: Int,
    shortLabel: String,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.radialGradient(
                    colors = if (isPressed) listOf(
                        GlassAccentCyan.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.25f)
                    ) else if (zoneId == 5) listOf(
                        GlassAccentCyan.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.10f)
                    ) else listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.04f)
                    )
                )
            )
            .border(
                BorderStroke(
                    1.2.dp,
                    if (isPressed) Color.White else if (zoneId == 5) GlassAccentCyan.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.22f)
                ),
                RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = GlassAccentCyan),
                onClick = {
                    scope.launch {
                        scale.animateTo(0.90f, tween(60))
                        scale.animateTo(1f, tween(90))
                    }
                    onTap()
                }
            )
            .scale(scale.value),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (zoneId == 5) GlassAccentCyan.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.18f))
                    .border(1.dp, if (zoneId == 5) GlassAccentCyan else GlassWhiteBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$zoneId",
                    color = if (zoneId == 5) Color.White else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = shortLabel,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun KnockQuadrant(
    zoneId: Int,
    label: String,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.radialGradient(
                    colors = if (isPressed) listOf(
                        GlassAccentCyan.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.25f)
                    ) else listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                BorderStroke(
                    1.5.dp,
                    if (isPressed) Color.White else Color.White.copy(alpha = 0.25f)
                ),
                RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = GlassAccentCyan),
                onClick = {
                    scope.launch {
                        scale.animateTo(0.92f, tween(70))
                        scale.animateTo(1f, tween(110))
                    }
                    onTap()
                }
            )
            .scale(scale.value),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(1.dp, GlassWhiteBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$zoneId",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
private fun KnockCenterZone(
    onTap: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(scale.value)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = if (isPressed) listOf(
                        GlassAccentCyan,
                        GlassPrimaryBlue
                    ) else listOf(
                        Color.White.copy(alpha = 0.40f),
                        Color.White.copy(alpha = 0.18f)
                    )
                )
            )
            .border(
                BorderStroke(2.dp, if (isPressed) Color.White else GlassAccentCyan.copy(alpha = 0.9f)),
                CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                onClick = {
                    scope.launch {
                        scale.animateTo(0.86f, tween(60))
                        scale.animateTo(1f, tween(100))
                    }
                    onTap()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "CENTER",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Text(
                text = "5",
                color = GlassAccentCyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
