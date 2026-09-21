package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NLockApplication
import com.example.data.model.KnockLayout
import com.example.data.model.KnockLength
import com.example.data.model.LockType
import com.example.data.model.PinLength
import com.example.ui.components.BackgroundWrapper
import com.example.ui.components.GlassCard
import com.example.ui.components.KnockLockView
import com.example.ui.components.PatternLockView
import com.example.ui.components.PinDotsDisplay
import com.example.ui.components.PinKeypad
import com.example.ui.theme.GlassAccentCyan
import com.example.ui.theme.GlassCyanGlow
import com.example.ui.theme.GlassPrimaryBlue
import com.example.ui.theme.GlassWhiteBorder
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed

@Composable
fun ChangeLockScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = NLockApplication.instance.repository
    val preferences = NLockApplication.instance.preferences
    val themeMode by preferences.themeFlow.collectAsState()

    var selectedLockType by remember { mutableStateOf(preferences.getLockType()) }

    // PIN Setup & Confirm State
    var selectedPinLength by remember { mutableStateOf(preferences.pinLength) }
    var enteredPin by remember { mutableStateOf("") }
    var confirmedPin by remember { mutableStateOf("") }
    var isConfirmingPin by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf(false) }

    // Pattern Setup & Confirm State
    var firstPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isConfirmingPattern by remember { mutableStateOf(false) }
    var patternError by remember { mutableStateOf(false) }

    // Knock Setup & Confirm State
    var selectedKnockLayout by remember { mutableStateOf(preferences.knockLayout) }
    var selectedKnockLength by remember { mutableStateOf(preferences.knockLength) }
    var firstKnock by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isConfirmingKnock by remember { mutableStateOf(false) }
    var knockError by remember { mutableStateOf(false) }

    // Completion State
    var isSaved by remember { mutableStateOf(false) }

    BackgroundWrapper(themeMode = themeMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Configure Lock Method",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Set up your active security credentials",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Lock Method Selector (PIN, Pattern, Knock)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.dp, GlassWhiteBorder, RoundedCornerShape(18.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LockType.entries.forEach { type ->
                    val isSelected = selectedLockType == type
                    val icon = when (type) {
                        LockType.PIN -> Icons.Default.Pin
                        LockType.PATTERN -> Icons.Default.Security
                        LockType.KNOCK -> Icons.Default.TouchApp
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                        Brush.horizontalGradient(listOf(GlassAccentCyan, GlassPrimaryBlue)),
                                        RoundedCornerShape(14.dp)
                                    )
                                } else {
                                    Modifier.background(Color.Transparent)
                                }
                            )
                            .border(
                                1.5.dp,
                                if (isSelected) Color.White else Color.Transparent,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(
                                indication = ripple(color = Color.White),
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) {
                                selectedLockType = type
                                enteredPin = ""
                                confirmedPin = ""
                                isConfirmingPin = false
                                pinError = false

                                firstPattern = emptyList()
                                isConfirmingPattern = false
                                patternError = false

                                firstKnock = emptyList()
                                isConfirmingKnock = false
                                knockError = false

                                isSaved = false
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = type.displayName,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedContent(
                targetState = isSaved,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ChangeLockState"
            ) { saved ->
                if (saved) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(NeonGreen.copy(alpha = 0.2f))
                                        .border(2.dp, NeonGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "${selectedLockType.displayName} Configured!",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your credentials have been encrypted and saved securely with Android Keystore.",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            isSaved = false
                                            enteredPin = ""
                                            confirmedPin = ""
                                            isConfirmingPin = false
                                            firstPattern = emptyList()
                                            isConfirmingPattern = false
                                            firstKnock = emptyList()
                                            isConfirmingKnock = false
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = BorderStroke(1.dp, GlassWhiteBorder)
                                    ) {
                                        Text("Re-configure")
                                    }
                                    Button(
                                        onClick = onNavigateBack,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan)
                                    ) {
                                        Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    when (selectedLockType) {
                        LockType.PIN -> {
                            val targetPinLen = selectedPinLength.length
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // PIN Length selector
                                Row(
                                    modifier = Modifier.padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PinLength.entries.forEach { len ->
                                        val isSel = selectedPinLength == len
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSel) GlassAccentCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f))
                                                .border(1.dp, if (isSel) GlassAccentCyan else Color.Transparent, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    selectedPinLength = len
                                                    enteredPin = ""
                                                    confirmedPin = ""
                                                    isConfirmingPin = false
                                                    pinError = false
                                                }
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "${len.length} Digits",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = if (!isConfirmingPin) "Step 1: Enter new ${targetPinLen}-digit PIN" else "Step 2: Confirm your PIN",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (pinError) "PINs did not match. Please try again." else "Numeric security code",
                                    color = if (pinError) NeonRed else Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                PinDotsDisplay(
                                    pinLength = targetPinLen,
                                    enteredCount = if (isConfirmingPin) confirmedPin.length else enteredPin.length,
                                    isError = pinError
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                PinKeypad(
                                    onDigitClick = { digit ->
                                        pinError = false
                                        if (!isConfirmingPin) {
                                            if (enteredPin.length < targetPinLen) {
                                                enteredPin += digit
                                                if (enteredPin.length == targetPinLen) {
                                                    isConfirmingPin = true
                                                }
                                            }
                                        } else {
                                            if (confirmedPin.length < targetPinLen) {
                                                confirmedPin += digit
                                                if (confirmedPin.length == targetPinLen) {
                                                    if (confirmedPin == enteredPin) {
                                                        repository.savePin(enteredPin, selectedPinLength)
                                                        preferences.setLockType(LockType.PIN)
                                                        isSaved = true
                                                        Toast.makeText(context, "PIN updated successfully", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        pinError = true
                                                        confirmedPin = ""
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onDeleteClick = {
                                        pinError = false
                                        if (isConfirmingPin) {
                                            if (confirmedPin.isNotEmpty()) {
                                                confirmedPin = confirmedPin.dropLast(1)
                                            } else {
                                                isConfirmingPin = false
                                            }
                                        } else {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                        }
                                    },
                                    showBiometric = false
                                )
                            }
                        }
                        LockType.PATTERN -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (!isConfirmingPattern) "Step 1: Draw your new pattern" else "Step 2: Confirm your pattern",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (patternError) {
                                        "Patterns did not match. Please redraw."
                                    } else if (!isConfirmingPattern) {
                                        "Connect at least 4 dots"
                                    } else {
                                        "Redraw the exact pattern to confirm"
                                    },
                                    color = if (patternError) NeonRed else Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                PatternLockView(
                                    onPatternComplete = { points ->
                                        patternError = false
                                        if (!isConfirmingPattern) {
                                            if (points.size >= 4) {
                                                firstPattern = points
                                                isConfirmingPattern = true
                                            } else {
                                                patternError = true
                                                Toast.makeText(context, "Connect at least 4 dots", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            if (points == firstPattern) {
                                                repository.savePattern(points)
                                                preferences.setLockType(LockType.PATTERN)
                                                isSaved = true
                                                Toast.makeText(context, "Pattern updated successfully", Toast.LENGTH_SHORT).show()
                                            } else {
                                                patternError = true
                                                isConfirmingPattern = false
                                                firstPattern = emptyList()
                                                Toast.makeText(context, "Patterns did not match. Try again.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    isError = patternError
                                )
                            }
                        }
                        LockType.KNOCK -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Knock Layout Selector
                                Row(
                                    modifier = Modifier.padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    KnockLayout.entries.forEach { layout ->
                                        val isSel = selectedKnockLayout == layout
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) GlassAccentCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f))
                                                .border(1.dp, if (isSel) GlassAccentCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedKnockLayout = layout
                                                    firstKnock = emptyList()
                                                    isConfirmingKnock = false
                                                    knockError = false
                                                }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = layout.displayName,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                // Knock Length Selector
                                Row(
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    KnockLength.entries.forEach { len ->
                                        val isSel = selectedKnockLength == len
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) GlassAccentCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f))
                                                .border(1.dp, if (isSel) GlassAccentCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedKnockLength = len
                                                    firstKnock = emptyList()
                                                    isConfirmingKnock = false
                                                    knockError = false
                                                }
                                                .padding(horizontal = 12.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "${len.length} Taps",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                val targetKnockLen = selectedKnockLength.length
                                Text(
                                    text = if (!isConfirmingKnock) "Step 1: Tap your ${targetKnockLen}-knock sequence" else "Step 2: Confirm your knock sequence",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (knockError) {
                                        "Sequences did not match. Please repeat."
                                    } else if (!isConfirmingKnock) {
                                        "Tap quadrants in sequence (${selectedKnockLayout.displayName})"
                                    } else {
                                        "Repeat the exact sequence to confirm"
                                    },
                                    color = if (knockError) NeonRed else Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                KnockLockView(
                                    onKnockComplete = { sequence ->
                                        knockError = false
                                        if (!isConfirmingKnock) {
                                            firstKnock = sequence
                                            isConfirmingKnock = true
                                        } else {
                                            if (sequence == firstKnock) {
                                                repository.saveKnock(sequence, selectedKnockLayout, selectedKnockLength)
                                                preferences.setLockType(LockType.KNOCK)
                                                isSaved = true
                                                Toast.makeText(context, "Knock sequence updated successfully", Toast.LENGTH_SHORT).show()
                                            } else {
                                                knockError = true
                                                isConfirmingKnock = false
                                                firstKnock = emptyList()
                                                Toast.makeText(context, "Sequences did not match. Try again.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    sequenceTargetLength = targetKnockLen,
                                    layout = selectedKnockLayout,
                                    isError = knockError
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
