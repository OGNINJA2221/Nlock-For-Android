package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassAccentCyan
import com.example.ui.theme.GlassWhiteBorder
import com.example.ui.theme.GlassWhiteHigh

@Composable
fun PinDotsDisplay(
    pinLength: Int,
    enteredCount: Int,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val totalDots = if (pinLength > 0) pinLength else 4
    val dotSpacing = if (totalDots > 6) 10.dp else 16.dp
    val dotSize = if (totalDots > 6) 14.dp else 16.dp

    Row(
        modifier = modifier.padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalDots) {
            val isFilled = i < enteredCount
            val scale by animateFloatAsState(
                targetValue = if (isFilled) 1.25f else 1.0f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
                label = "dot_scale"
            )

            val dotColor = when {
                isError && isFilled -> Color(0xFFFF3366)
                isError -> Color(0xFFFF3366).copy(alpha = 0.15f)
                isFilled -> GlassAccentCyan
                else -> Color.White.copy(alpha = 0.25f)
            }

            val borderColor = when {
                isError -> Color(0xFFFF3366)
                isFilled -> Color.White
                else -> GlassWhiteBorder
            }

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(
                        1.5.dp,
                        borderColor,
                        CircleShape
                    )
            )
        }
    }
}

@Composable
fun PinKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onBiometricClick: (() -> Unit)? = null,
    showBiometric: Boolean = true,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("BIOMETRIC", "0", "DELETE")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in keys) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (key in row) {
                    when (key) {
                        "BIOMETRIC" -> {
                            if (showBiometric && onBiometricClick != null) {
                                GlassCircleButton(
                                    onClick = onBiometricClick,
                                    enabled = enabled,
                                    modifier = Modifier.size(78.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometric Unlock",
                                        tint = if (enabled) GlassAccentCyan else GlassAccentCyan.copy(alpha = 0.3f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(78.dp))
                            }
                        }
                        "DELETE" -> {
                            GlassCircleButton(
                                onClick = onDeleteClick,
                                enabled = enabled,
                                modifier = Modifier.size(78.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Delete Digit",
                                    tint = if (enabled) Color.White else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        else -> {
                            GlassCircleButton(
                                onClick = { onDigitClick(key) },
                                enabled = enabled,
                                modifier = Modifier.size(78.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = key,
                                        color = if (enabled) Color.White else Color.White.copy(alpha = 0.3f),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val subLetters = when (key) {
                                        "2" -> "ABC"
                                        "3" -> "DEF"
                                        "4" -> "GHI"
                                        "5" -> "JKL"
                                        "6" -> "MNO"
                                        "7" -> "PQRS"
                                        "8" -> "TUV"
                                        "9" -> "WXYZ"
                                        else -> ""
                                    }
                                    if (subLetters.isNotEmpty()) {
                                        Text(
                                            text = subLetters,
                                            color = if (enabled) Color.White.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.2f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 1.sp
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
