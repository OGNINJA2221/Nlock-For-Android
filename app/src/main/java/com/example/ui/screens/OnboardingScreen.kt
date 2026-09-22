package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NLockApplication
import com.example.R
import com.example.data.model.KnockLayout
import com.example.data.model.KnockLength
import com.example.data.model.LockType
import com.example.data.model.PinLength
import com.example.ui.components.BackgroundWrapper
import com.example.ui.components.GlassCard
import com.example.ui.components.KnockLockView
import com.example.ui.components.PasswordRecoverySetupView
import com.example.ui.components.PatternLockView
import com.example.ui.components.PinDotsDisplay
import com.example.ui.components.PinKeypad
import com.example.ui.theme.GlassAccentCyan
import com.example.ui.theme.GlassWhiteBorder
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val repository = NLockApplication.instance.repository
    val securityManager = NLockApplication.instance.securityManager
    val preferences = NLockApplication.instance.preferences

    var currentPage by remember { mutableIntStateOf(0) }
    var selectedLockType by remember { mutableStateOf(LockType.PIN) }

    // Lock setup state
    var selectedPinLength by remember { mutableStateOf(PinLength.FOUR) }
    var enteredPin by remember { mutableStateOf("") }
    var confirmedPin by remember { mutableStateOf("") }
    var isConfirmingPin by remember { mutableStateOf(false) }
    var pinSetupError by remember { mutableStateOf(false) }

    var enteredPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isConfirmingPattern by remember { mutableStateOf(false) }

    var selectedKnockLayout by remember { mutableStateOf(KnockLayout.GRID_2X2_CENTER) }
    var selectedKnockLength by remember { mutableStateOf(KnockLength.FOUR) }
    var enteredKnock by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isConfirmingKnock by remember { mutableStateOf(false) }

    var lockSetupDone by remember { mutableStateOf(false) }
    var recoverySetupDone by remember { mutableStateOf(preferences.isRecoveryConfigured) }

    var hasCameraPermission by remember { mutableStateOf(securityManager.hasCameraPermission()) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            preferences.isIntruderSelfieEnabled = true
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    BackgroundWrapper {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Stepper dots (6 total steps: Welcome, Privacy, Lock Method, Recovery Setup, Permissions, Complete)
            Row(
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 0..5) {
                    Box(
                        modifier = Modifier
                            .size(if (i == currentPage) 28.dp else 10.dp, 10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (i == currentPage) GlassAccentCyan else Color.White.copy(alpha = 0.25f))
                    )
                }
            }

            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "onboarding_pages"
            ) { page ->
                when (page) {
                    0 -> OnboardingWelcomePage()
                    1 -> OnboardingPrivacyPage()
                    2 -> OnboardingLockSetupPage(
                        selectedLockType = selectedLockType,
                        onSelectType = { type ->
                            selectedLockType = type
                            enteredPin = ""
                            confirmedPin = ""
                            isConfirmingPin = false
                            pinSetupError = false
                            enteredPattern = emptyList()
                            isConfirmingPattern = false
                            enteredKnock = emptyList()
                            isConfirmingKnock = false
                            lockSetupDone = false
                        },
                        selectedPinLength = selectedPinLength,
                        onSelectPinLength = { len ->
                            selectedPinLength = len
                            enteredPin = ""
                            confirmedPin = ""
                            isConfirmingPin = false
                            pinSetupError = false
                            lockSetupDone = false
                        },
                        enteredPin = if (isConfirmingPin) confirmedPin else enteredPin,
                        isConfirmingPin = isConfirmingPin,
                        pinSetupError = pinSetupError,
                        onPinDigit = { digit ->
                            val targetLen = selectedPinLength.length
                            if (!isConfirmingPin) {
                                if (enteredPin.length < targetLen) {
                                    enteredPin += digit
                                    if (enteredPin.length == targetLen) {
                                        isConfirmingPin = true
                                    }
                                }
                            } else {
                                if (confirmedPin.length < targetLen) {
                                    confirmedPin += digit
                                    if (confirmedPin.length == targetLen) {
                                        if (confirmedPin == enteredPin) {
                                            repository.savePin(enteredPin, selectedPinLength)
                                            preferences.setLockType(LockType.PIN)
                                            lockSetupDone = true
                                        } else {
                                            pinSetupError = true
                                            confirmedPin = ""
                                        }
                                    }
                                }
                            }
                        },
                        onPinDelete = {
                            if (isConfirmingPin) {
                                if (confirmedPin.isNotEmpty()) confirmedPin = confirmedPin.dropLast(1)
                                else isConfirmingPin = false
                            } else {
                                if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                            }
                            pinSetupError = false
                        },
                        onPatternSet = { points ->
                            if (!isConfirmingPattern) {
                                enteredPattern = points
                                isConfirmingPattern = true
                            } else {
                                if (points == enteredPattern) {
                                    repository.savePattern(points)
                                    preferences.setLockType(LockType.PATTERN)
                                    lockSetupDone = true
                                } else {
                                    isConfirmingPattern = false
                                    enteredPattern = emptyList()
                                }
                            }
                        },
                        isConfirmingPattern = isConfirmingPattern,
                        selectedKnockLayout = selectedKnockLayout,
                        onSelectKnockLayout = { layout ->
                            selectedKnockLayout = layout
                            enteredKnock = emptyList()
                            isConfirmingKnock = false
                            lockSetupDone = false
                        },
                        selectedKnockLength = selectedKnockLength,
                        onSelectKnockLength = { len ->
                            selectedKnockLength = len
                            enteredKnock = emptyList()
                            isConfirmingKnock = false
                            lockSetupDone = false
                        },
                        onKnockSet = { sequence ->
                            if (!isConfirmingKnock) {
                                enteredKnock = sequence
                                isConfirmingKnock = true
                            } else {
                                if (sequence == enteredKnock) {
                                    repository.saveKnock(sequence, selectedKnockLayout, selectedKnockLength)
                                    preferences.setLockType(LockType.KNOCK)
                                    lockSetupDone = true
                                } else {
                                    isConfirmingKnock = false
                                    enteredKnock = emptyList()
                                }
                            }
                        },
                        isConfirmingKnock = isConfirmingKnock,
                        lockSetupDone = lockSetupDone
                    )
                    3 -> PasswordRecoverySetupView(
                        onSaveRecovery = { q1, a1, q2, a2, q3, a3 ->
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            repository.saveRecoverySetup(q1, a1, q2, a2, q3, a3)
                            recoverySetupDone = true
                            Toast.makeText(context, "Password recovery setup saved successfully", Toast.LENGTH_SHORT).show()
                        }
                    )
                    4 -> OnboardingPermissionsPage(
                        securityManager = securityManager,
                        hasCamera = hasCameraPermission,
                        onRequestCameraPermission = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onOpenAccessibilitySettings = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        onOpenUsageSettings = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        },
                        onOpenOverlaySettings = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )
                    5 -> OnboardingCompletedPage()
                }
            }

            // Bottom Navigation Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            currentPage--
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(40.dp))
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()

                        if (currentPage < 5) {
                            if (currentPage == 2 && !lockSetupDone) {
                                // Default PIN 1234 if skipped
                                repository.savePin("1234", PinLength.FOUR)
                                preferences.setLockType(LockType.PIN)
                                lockSetupDone = true
                            }
                            if (currentPage == 3 && !recoverySetupDone) {
                                Toast.makeText(
                                    context,
                                    "Password Recovery Setup is mandatory. Please answer 3 questions to continue.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@Button
                            }
                            currentPage++
                        } else {
                            preferences.isOnboardingCompleted = true
                            onFinished()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassAccentCyan,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = if (currentPage == 5) "Get Started" else "Next",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingWelcomePage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Hand-drawn N Logo Branding
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .border(2.dp, GlassAccentCyan, RoundedCornerShape(36.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.nlock_logo),
                contentDescription = "N Lock Logo",
                modifier = Modifier
                    .size(95.dp)
                    .clip(RoundedCornerShape(26.dp))
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Welcome to N Lock",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Simple. Secure. Personal.",
            color = GlassAccentCyan,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Safeguard your messages, gallery, social media, and banking apps with military-grade encryption and glassmorphism elegance.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OnboardingPrivacyPage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Protect Your Privacy",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        val features = listOf(
            Triple(Icons.Default.Pin, "PIN & Pattern Lock", "Classic, highly responsive security options with custom length."),
            Triple(Icons.Default.TouchApp, "Exclusive Knock Lock", "Tap your secret custom sequence anywhere on the screen quadrants."),
            Triple(Icons.Default.Security, "Intruder Selfie", "Captures photos of anyone attempting 3 unauthorized unlocks."),
            Triple(Icons.Default.VisibilityOff, "Fake Crash & Stealth", "Disguises app locks as system crashes and hides the launcher icon.")
        )

        for ((icon, title, desc) in features) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GlassAccentCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = GlassAccentCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(desc, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingLockSetupPage(
    selectedLockType: LockType,
    onSelectType: (LockType) -> Unit,
    selectedPinLength: PinLength,
    onSelectPinLength: (PinLength) -> Unit,
    enteredPin: String,
    isConfirmingPin: Boolean,
    pinSetupError: Boolean,
    onPinDigit: (String) -> Unit,
    onPinDelete: () -> Unit,
    onPatternSet: (List<Int>) -> Unit,
    isConfirmingPattern: Boolean,
    selectedKnockLayout: KnockLayout,
    onSelectKnockLayout: (KnockLayout) -> Unit,
    selectedKnockLength: KnockLength,
    onSelectKnockLength: (KnockLength) -> Unit,
    onKnockSet: (List<Int>) -> Unit,
    isConfirmingKnock: Boolean,
    lockSetupDone: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose Lock Method",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Lock Method Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LockType.entries.forEach { type ->
                val isSelected = selectedLockType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) GlassAccentCyan else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) Color.White else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectType(type) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type.displayName,
                        color = if (isSelected) Color.Black else Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (lockSetupDone) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${selectedLockType.displayName} Configured!",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onSelectType(selectedLockType) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Change Method")
                    }
                }
            }
        } else {
            when (selectedLockType) {
                LockType.PIN -> {
                    // PIN Length selector
                    Row(
                        modifier = Modifier.padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PinLength.entries.forEach { len ->
                            val isSel = selectedPinLength == len
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) GlassAccentCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, if (isSel) GlassAccentCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { onSelectPinLength(len) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${len.length} Digits",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    val targetPinLen = selectedPinLength.length
                    Text(
                        text = if (!isConfirmingPin) "Enter a ${targetPinLen}-digit PIN" else "Confirm your PIN",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PinDotsDisplay(
                        pinLength = targetPinLen,
                        enteredCount = enteredPin.length,
                        isError = pinSetupError
                    )
                    PinKeypad(
                        onDigitClick = onPinDigit,
                        onDeleteClick = onPinDelete,
                        showBiometric = false
                    )
                }
                LockType.PATTERN -> {
                    Text(
                        text = if (!isConfirmingPattern) "Draw a pattern (min 4 dots)" else "Confirm pattern",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )
                    PatternLockView(onPatternComplete = onPatternSet)
                }
                LockType.KNOCK -> {
                    // Knock Layout Selector
                    Row(
                        modifier = Modifier.padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KnockLayout.entries.forEach { layout ->
                            val isSel = selectedKnockLayout == layout
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) GlassAccentCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, if (isSel) GlassAccentCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { onSelectKnockLayout(layout) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
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
                        modifier = Modifier.padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KnockLength.entries.forEach { len ->
                            val isSel = selectedKnockLength == len
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) GlassAccentCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, if (isSel) GlassAccentCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { onSelectKnockLength(len) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
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
                        text = if (!isConfirmingKnock) "Tap $targetKnockLen knocks in sequence" else "Confirm knock sequence",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    KnockLockView(
                        onKnockComplete = onKnockSet,
                        sequenceTargetLength = targetKnockLen,
                        layout = selectedKnockLayout
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPermissionsPage(
    securityManager: com.example.security.SecurityManager,
    hasCamera: Boolean,
    onRequestCameraPermission: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    val hasUsage = securityManager.hasUsageStatsPermission()
    val hasOverlay = securityManager.hasOverlayPermission()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Grant Permissions",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "N Lock requires standard Android permissions to detect when protected apps open and capture intruder selfies.",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Accessibility Service Card (Primary)
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Accessibility Service", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Primary real-time detection for protected apps",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = onOpenAccessibilitySettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassAccentCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Usage Access Permission Card
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Usage Access", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Allows N Lock to recognize foreground apps",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = onOpenUsageSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasUsage) NeonGreen else GlassAccentCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (hasUsage) "Granted" else "Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Draw Over Other Apps Card
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Display Over Apps", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Shows the glass lock screen over target apps",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = onOpenOverlaySettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasOverlay) NeonGreen else GlassAccentCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (hasOverlay) "Granted" else "Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Camera Permission Card for Intruder Report
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Camera (Intruder Report)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Captures photos of unauthorized users after 3 failed unlock attempts",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = onRequestCameraPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasCamera) NeonGreen else GlassAccentCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (hasCamera) "Granted" else "Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun OnboardingCompletedPage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = GlassAccentCyan,
            modifier = Modifier.size(90.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "You're All Set!",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "N Lock is actively guarding your digital sanctuary. Tap below to access your security dashboard and manage locked apps.",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}
