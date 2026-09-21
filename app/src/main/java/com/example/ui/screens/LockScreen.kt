package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.NLockApplication
import com.example.R
import com.example.data.model.LockType
import com.example.security.CameraHelper
import com.example.ui.components.BackgroundWrapper
import com.example.ui.components.FakeCrashDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.KnockLockView
import com.example.ui.components.PasswordRecoveryDialog
import com.example.ui.components.PatternLockView
import com.example.ui.components.PinDotsDisplay
import com.example.ui.components.PinKeypad
import com.example.ui.components.UnlockSuccessSparkles
import com.example.ui.theme.GlassAccentCyan
import com.example.ui.theme.NeonRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LockScreen(
    targetAppName: String,
    targetPackageName: String,
    onUnlocked: () -> Unit,
    onCancelled: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = NLockApplication.instance.repository
    val preferences = NLockApplication.instance.preferences
    val scope = rememberCoroutineScope()
    val cameraHelper = remember { CameraHelper(context) }

    val themeMode by preferences.themeFlow.collectAsState()
    var activeLockType by remember { mutableStateOf(preferences.getLockType()) }

    var failedAttempts by remember { mutableIntStateOf(preferences.failedAttemptsCount) }
    var cooldownRemainingSeconds by remember { mutableLongStateOf(preferences.getRemainingCooldownSeconds()) }
    val isCooldownActive = cooldownRemainingSeconds > 0

    // Countdown timer for Brute Force Protection
    LaunchedEffect(cooldownRemainingSeconds) {
        if (cooldownRemainingSeconds > 0) {
            delay(1000)
            cooldownRemainingSeconds = preferences.getRemainingCooldownSeconds()
        }
    }

    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var isUnlocked by remember { mutableStateOf(false) }
    var showFakeCrash by remember { mutableStateOf(preferences.isFakeCrashEnabled) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }

    fun triggerVibration() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun handleFailedAttempt(method: String = activeLockType.displayName) {
        // Immediate clearing of entered PIN & reset progress (the incorrect PIN must never remain visible)
        enteredPin = ""
        isError = true
        errorMessage = "Incorrect $method. Please try again."
        triggerVibration()

        // Track failed attempt & activate cooldown thresholds (5/10/15/20 attempts -> 30s/60s/5m/15m)
        val cooldownSeconds = preferences.recordFailedAttempt()
        failedAttempts = preferences.failedAttemptsCount
        if (cooldownSeconds > 0) {
            cooldownRemainingSeconds = cooldownSeconds
        }

        scope.launch {
            // Shake animation
            repeat(3) {
                shakeOffset.animateTo(22f, tween(40))
                shakeOffset.animateTo(-22f, tween(40))
            }
            shakeOffset.animateTo(0f, spring())

            repository.recordUnlockEvent(targetAppName, false)
            if (failedAttempts >= preferences.intruderThreshold && preferences.isIntruderSelfieEnabled) {
                if (cameraHelper.hasCameraPermission()) {
                    cameraHelper.captureIntruderPhoto(
                        lifecycleOwner = lifecycleOwner,
                        onCaptured = { path ->
                            scope.launch {
                                repository.recordIntruderAttempt(
                                    targetApp = targetAppName,
                                    targetPackage = targetPackageName,
                                    attempts = failedAttempts,
                                    photoPath = path,
                                    lockType = method
                                )
                            }
                        },
                        onError = {
                            scope.launch {
                                repository.recordIntruderAttempt(
                                    targetApp = targetAppName,
                                    targetPackage = targetPackageName,
                                    attempts = failedAttempts,
                                    photoPath = null,
                                    lockType = method
                                )
                            }
                        }
                    )
                } else {
                    repository.recordIntruderAttempt(
                        targetApp = targetAppName,
                        targetPackage = targetPackageName,
                        attempts = failedAttempts,
                        photoPath = null,
                        lockType = method
                    )
                }
            }
            delay(800)
            isError = false
        }
    }

    fun handleSuccess() {
        preferences.resetFailedAttempts()
        failedAttempts = 0
        cooldownRemainingSeconds = 0
        isUnlocked = true
        scope.launch {
            repository.recordUnlockEvent(targetAppName, true)
        }
    }

    fun triggerBiometricPrompt() {
        if (isCooldownActive) return
        val fragmentActivity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            fragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    handleSuccess()
                }

                override fun onAuthenticationFailed() {
                    handleFailedAttempt("Fingerprint")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_LOCKOUT ||
                        errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT
                    ) {
                        handleFailedAttempt("Fingerprint")
                    }
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock $targetAppName")
            .setSubtitle("Confirm your biometric identity")
            .setNegativeButtonText("Use ${activeLockType.displayName}")
            .build()

        prompt.authenticate(promptInfo)
    }

    // Auto-launch biometric on screen open if enabled
    LaunchedEffect(Unit) {
        if (preferences.isBiometricEnabled && !showFakeCrash && !isCooldownActive) {
            triggerBiometricPrompt()
        }
    }

    if (showFakeCrash) {
        FakeCrashDialog(
            targetAppName = targetAppName,
            onSecretUnlocked = { showFakeCrash = false },
            onNormalDismiss = onCancelled
        )
    }

    BackgroundWrapper(themeMode = themeMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top App Identifier & N Logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(
                            1.5.dp,
                            if (isError) NeonRed else GlassAccentCyan,
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.nlock_logo),
                        contentDescription = "N Lock",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = targetAppName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (isError) (errorMessage ?: "Incorrect ${activeLockType.displayName}") else "Protected by N Lock",
                    color = if (isError) NeonRed else GlassAccentCyan,
                    fontSize = 13.sp,
                    fontWeight = if (isError) FontWeight.Bold else FontWeight.Medium
                )

                if (isCooldownActive) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(NeonRed.copy(alpha = 0.18f))
                            .border(1.5.dp, NeonRed.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
                            .padding(vertical = 10.dp, horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = NeonRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Too many incorrect attempts.",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val mins = cooldownRemainingSeconds / 60
                            val secs = cooldownRemainingSeconds % 60
                            val timeText = String.format("%02d:%02d", mins, secs)
                            Text(
                                text = "Try again in $timeText",
                                color = NeonRed,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                } else if (failedAttempts > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonRed.copy(alpha = 0.2f))
                            .border(1.dp, NeonRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = NeonRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$failedAttempts Failed Attempt${if (failedAttempts > 1) "s" else ""}",
                            color = NeonRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Middle: Active Lock Method View with Shake & Red Glow feedback
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                contentAlignment = Alignment.Center
            ) {
                when (activeLockType) {
                    LockType.PIN -> {
                        val pinLen = preferences.pinLength.length
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PinDotsDisplay(
                                pinLength = pinLen,
                                enteredCount = enteredPin.length,
                                isError = isError
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            PinKeypad(
                                enabled = !isCooldownActive,
                                onDigitClick = { digit ->
                                    if (!isCooldownActive && enteredPin.length < pinLen) {
                                        enteredPin += digit
                                        if (enteredPin.length == pinLen) {
                                            if (repository.verifyPin(enteredPin)) {
                                                handleSuccess()
                                            } else {
                                                handleFailedAttempt(LockType.PIN.displayName)
                                            }
                                        }
                                    }
                                },
                                onDeleteClick = {
                                    if (!isCooldownActive && enteredPin.isNotEmpty()) {
                                        enteredPin = enteredPin.dropLast(1)
                                    }
                                    isError = false
                                },
                                onBiometricClick = { if (!isCooldownActive) triggerBiometricPrompt() },
                                showBiometric = preferences.isBiometricEnabled && !isCooldownActive
                            )
                        }
                    }
                    LockType.PATTERN -> {
                        PatternLockView(
                            onPatternComplete = { points ->
                                if (!isCooldownActive) {
                                    if (repository.verifyPattern(points)) {
                                        handleSuccess()
                                    } else {
                                        handleFailedAttempt(LockType.PATTERN.displayName)
                                    }
                                }
                            },
                            isError = isError,
                            enabled = !isCooldownActive
                        )
                    }
                    LockType.KNOCK -> {
                        KnockLockView(
                            onKnockComplete = { sequence ->
                                if (!isCooldownActive) {
                                    if (repository.verifyKnock(sequence)) {
                                        handleSuccess()
                                    } else {
                                        handleFailedAttempt(LockType.KNOCK.displayName)
                                    }
                                }
                            },
                            sequenceTargetLength = preferences.knockLength.length,
                            layout = preferences.knockLayout,
                            isError = isError,
                            enabled = !isCooldownActive
                        )
                    }
                }
            }

            // Bottom Switch Method Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LockType.entries.forEach { type ->
                    val isCurrent = activeLockType == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) GlassAccentCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                            .border(
                                1.dp,
                                if (isCurrent) GlassAccentCyan else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = !isCooldownActive) {
                                activeLockType = type
                                enteredPin = ""
                                isError = false
                                errorMessage = null
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = type.displayName,
                            color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Forgot Password Recovery Button
            TextButton(
                onClick = {
                    showRecoveryDialog = true
                }
            ) {
                Text(
                    text = "Forgot Password?",
                    color = GlassAccentCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (showRecoveryDialog) {
            PasswordRecoveryDialog(
                onDismiss = { showRecoveryDialog = false },
                onRecoverySuccess = {
                    showRecoveryDialog = false
                    activeLockType = preferences.getLockType()
                    enteredPin = ""
                    isError = false
                    errorMessage = null
                    handleSuccess()
                }
            )
        }

        // Particle Sparkle celebration on unlock
        if (isUnlocked) {
            UnlockSuccessSparkles(
                onFinished = onUnlocked
            )
        }
    }
}
