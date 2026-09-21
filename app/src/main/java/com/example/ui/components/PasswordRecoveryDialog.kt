package com.example.ui.components

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.NLockApplication
import com.example.data.model.KnockLayout
import com.example.data.model.KnockLength
import com.example.data.model.LockType
import com.example.data.model.PinLength
import com.example.ui.theme.GlassAccentCyan
import com.example.ui.theme.GlassPrimaryBlue
import com.example.ui.theme.GlassWhiteBorder
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed

enum class RecoveryStep {
    QUESTIONS,
    DEVICE_AUTH,
    RESET_LOCK
}

@Composable
fun PasswordRecoveryDialog(
    onDismiss: () -> Unit,
    onRecoverySuccess: () -> Unit
) {
    val context = LocalContext.current
    val repository = NLockApplication.instance.repository
    val preferences = NLockApplication.instance.preferences

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun safeDismiss() {
        focusManager.clearFocus()
        keyboardController?.hide()
        onDismiss()
    }

    fun safeRecoverySuccess() {
        focusManager.clearFocus()
        keyboardController?.hide()
        onRecoverySuccess()
    }

    var currentStep by remember { mutableStateOf(RecoveryStep.QUESTIONS) }

    // Step 1: Questions & Answers
    val q1 = preferences.recoveryQuestion1.ifBlank { "What was your first pet's name?" }
    val q2 = preferences.recoveryQuestion2.ifBlank { "What city were you born in?" }
    val q3 = preferences.recoveryQuestion3.ifBlank { "What is your favorite food?" }

    var a1 by remember { mutableStateOf("") }
    var a2 by remember { mutableStateOf("") }
    var a3 by remember { mutableStateOf("") }
    var questionsError by remember { mutableStateOf<String?>(null) }

    // Step 2: Device authentication
    var deviceAuthFailed by remember { mutableStateOf(false) }

    val deviceCredentialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentStep = RecoveryStep.RESET_LOCK
        } else {
            deviceAuthFailed = true
        }
    }

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

    fun launchDeviceAuthentication() {
        deviceAuthFailed = false
        val fragmentActivity = context as? FragmentActivity
        if (fragmentActivity != null) {
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(
                fragmentActivity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        currentStep = RecoveryStep.RESET_LOCK
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        // Fallback to KeyguardManager device credential intent
                        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                        val intent = keyguardManager?.createConfirmDeviceCredentialIntent(
                            "Verify Identity",
                            "Confirm your device lock screen (PIN, Pattern, or Password) to reset N Lock"
                        )
                        if (intent != null) {
                            deviceCredentialLauncher.launch(intent)
                        } else {
                            // If device has no credentials setup, allow bypass since questions were verified
                            currentStep = RecoveryStep.RESET_LOCK
                        }
                    }

                    override fun onAuthenticationFailed() {
                        deviceAuthFailed = true
                    }
                }
            )

            try {
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Verify Identity")
                    .setSubtitle("Confirm your device lock screen to reset N Lock")
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    .build()

                prompt.authenticate(promptInfo)
            } catch (e: Exception) {
                val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                val intent = keyguardManager?.createConfirmDeviceCredentialIntent(
                    "Verify Identity",
                    "Confirm your device lock screen to reset N Lock"
                )
                if (intent != null) {
                    deviceCredentialLauncher.launch(intent)
                } else {
                    currentStep = RecoveryStep.RESET_LOCK
                }
            }
        } else {
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            val intent = keyguardManager?.createConfirmDeviceCredentialIntent(
                "Verify Identity",
                "Confirm your device lock screen to reset N Lock"
            )
            if (intent != null) {
                deviceCredentialLauncher.launch(intent)
            } else {
                currentStep = RecoveryStep.RESET_LOCK
            }
        }
    }

    // Step 3: Reset lock credentials
    var newLockType by remember { mutableStateOf(LockType.PIN) }
    var newPinLength by remember { mutableStateOf(PinLength.FOUR) }
    var enteredPin by remember { mutableStateOf("") }
    var confirmedPin by remember { mutableStateOf("") }
    var isConfirmingPin by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf(false) }

    var enteredPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isConfirmingPattern by remember { mutableStateOf(false) }
    var patternError by remember { mutableStateOf(false) }

    var newKnockLayout by remember { mutableStateOf(KnockLayout.GRID_2X2_CENTER) }
    var newKnockLength by remember { mutableStateOf(KnockLength.FOUR) }
    var enteredKnock by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isConfirmingKnock by remember { mutableStateOf(false) }
    var knockError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { safeDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .imePadding()
                .clip(RoundedCornerShape(28.dp)),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, GlassAccentCyan.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(GlassAccentCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockReset,
                                contentDescription = null,
                                tint = GlassAccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Account Recovery",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (currentStep) {
                                    RecoveryStep.QUESTIONS -> "Step 1 of 3: Security Questions"
                                    RecoveryStep.DEVICE_AUTH -> "Step 2 of 3: Device Verification"
                                    RecoveryStep.RESET_LOCK -> "Step 3 of 3: Create New Lock"
                                },
                                color = GlassAccentCyan,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = { safeDismiss() }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stepper progress indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(if (currentStep >= RecoveryStep.QUESTIONS) GlassAccentCyan else Color.White.copy(alpha = 0.2f))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(if (currentStep >= RecoveryStep.DEVICE_AUTH) GlassAccentCyan else Color.White.copy(alpha = 0.2f))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(if (currentStep >= RecoveryStep.RESET_LOCK) GlassAccentCyan else Color.White.copy(alpha = 0.2f))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentStep) {
                        RecoveryStep.QUESTIONS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Answer all 3 security questions correctly to verify your identity.",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Question 1
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(q1, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = a1,
                                            onValueChange = { a1 = it; questionsError = null },
                                            label = { Text("Answer 1") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = GlassAccentCyan,
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                                focusedLabelColor = GlassAccentCyan,
                                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Question 2
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(q2, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = a2,
                                            onValueChange = { a2 = it; questionsError = null },
                                            label = { Text("Answer 2") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = GlassAccentCyan,
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                                focusedLabelColor = GlassAccentCyan,
                                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Question 3
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(q3, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = a3,
                                            onValueChange = { a3 = it; questionsError = null },
                                            label = { Text("Answer 3") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = GlassAccentCyan,
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                                focusedLabelColor = GlassAccentCyan,
                                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                                            )
                                        )
                                    }
                                }

                                if (questionsError != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = NeonRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(questionsError!!, color = NeonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()

                                        if (a1.isBlank() || a2.isBlank() || a3.isBlank()) {
                                            questionsError = "Please answer all 3 questions"
                                            triggerVibration()
                                            return@Button
                                        }

                                        val isConfigured = preferences.isRecoveryConfigured
                                        val isValid = if (isConfigured) {
                                            repository.verifyAllRecoveryAnswers(a1, a2, a3)
                                        } else {
                                            // Fallback if user hadn't configured 3 questions yet
                                            val oldHash = preferences.securityAnswerHash
                                            if (oldHash.isNullOrBlank()) {
                                                true
                                            } else {
                                                val enteredHash = NLockApplication.instance.cryptoManager.hashWithSalt(a1.trim().lowercase())
                                                enteredHash == oldHash
                                            }
                                        }

                                        if (isValid) {
                                            questionsError = null
                                            currentStep = RecoveryStep.DEVICE_AUTH
                                            launchDeviceAuthentication()
                                        } else {
                                            questionsError = "Incorrect answer(s). Please try again."
                                            triggerVibration()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan, contentColor = Color.Black)
                                ) {
                                    Text("Verify Answers", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        RecoveryStep.DEVICE_AUTH -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(GlassAccentCyan.copy(alpha = 0.15f))
                                        .border(2.dp, GlassAccentCyan, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = GlassAccentCyan,
                                        modifier = Modifier.size(46.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Text(
                                    text = "Device Authentication",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Confirm your device lock screen (Device PIN, Pattern, Password, or Biometric) to authorize reset.",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                if (deviceAuthFailed) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Authentication failed or cancelled. Please try again.",
                                        color = NeonRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = { launchDeviceAuthentication() },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan, contentColor = Color.Black)
                                ) {
                                    Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Authenticate with Device", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedButton(
                                    onClick = { currentStep = RecoveryStep.QUESTIONS },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Text("Back to Security Questions")
                                }
                            }
                        }

                        RecoveryStep.RESET_LOCK -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Choose and create your new lock method:",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Lock Type selector
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    LockType.entries.forEach { type ->
                                        val isSel = newLockType == type
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSel) GlassAccentCyan else Color.Transparent)
                                                .clickable {
                                                    newLockType = type
                                                    enteredPin = ""
                                                    confirmedPin = ""
                                                    isConfirmingPin = false
                                                    pinError = false
                                                    enteredPattern = emptyList()
                                                    isConfirmingPattern = false
                                                    patternError = false
                                                    enteredKnock = emptyList()
                                                    isConfirmingKnock = false
                                                    knockError = false
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                type.displayName,
                                                color = if (isSel) Color.Black else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                when (newLockType) {
                                    LockType.PIN -> {
                                        // Pin Length selector
                                        Row(
                                            modifier = Modifier.padding(bottom = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            PinLength.entries.forEach { len ->
                                                val isLen = newPinLength == len
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(if (isLen) GlassAccentCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f))
                                                        .border(1.dp, if (isLen) GlassAccentCyan else Color.Transparent, RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            newPinLength = len
                                                            enteredPin = ""
                                                            confirmedPin = ""
                                                            isConfirmingPin = false
                                                            pinError = false
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text("${len.length} Digits", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Text(
                                            text = if (!isConfirmingPin) "Enter new ${newPinLength.length}-digit PIN" else "Confirm new PIN",
                                            color = if (pinError) NeonRed else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        PinDotsDisplay(
                                            pinLength = newPinLength.length,
                                            enteredCount = if (isConfirmingPin) confirmedPin.length else enteredPin.length,
                                            isError = pinError
                                        )

                                        PinKeypad(
                                            onDigitClick = { digit ->
                                                pinError = false
                                                val target = newPinLength.length
                                                if (!isConfirmingPin) {
                                                    if (enteredPin.length < target) {
                                                        enteredPin += digit
                                                        if (enteredPin.length == target) isConfirmingPin = true
                                                    }
                                                } else {
                                                    if (confirmedPin.length < target) {
                                                        confirmedPin += digit
                                                        if (confirmedPin.length == target) {
                                                            if (confirmedPin == enteredPin) {
                                                                repository.savePin(enteredPin, newPinLength)
                                                                preferences.setLockType(LockType.PIN)
                                                                Toast.makeText(context, "New PIN configured & saved!", Toast.LENGTH_SHORT).show()
                                                                safeRecoverySuccess()
                                                            } else {
                                                                pinError = true
                                                                confirmedPin = ""
                                                                triggerVibration()
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            onDeleteClick = {
                                                pinError = false
                                                if (isConfirmingPin) {
                                                    if (confirmedPin.isNotEmpty()) confirmedPin = confirmedPin.dropLast(1)
                                                    else isConfirmingPin = false
                                                } else {
                                                    if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                                }
                                            },
                                            showBiometric = false
                                        )
                                    }

                                    LockType.PATTERN -> {
                                        Text(
                                            text = if (!isConfirmingPattern) "Draw your new pattern (connect at least 4 dots)" else "Redraw pattern to confirm",
                                            color = if (patternError) NeonRed else Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        PatternLockView(
                                            onPatternComplete = { points ->
                                                patternError = false
                                                if (!isConfirmingPattern) {
                                                    if (points.size >= 4) {
                                                        enteredPattern = points
                                                        isConfirmingPattern = true
                                                    } else {
                                                        patternError = true
                                                        triggerVibration()
                                                        Toast.makeText(context, "Connect at least 4 dots", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    if (points == enteredPattern) {
                                                        repository.savePattern(points)
                                                        preferences.setLockType(LockType.PATTERN)
                                                        Toast.makeText(context, "New Pattern configured & saved!", Toast.LENGTH_SHORT).show()
                                                        safeRecoverySuccess()
                                                    } else {
                                                        patternError = true
                                                        isConfirmingPattern = false
                                                        enteredPattern = emptyList()
                                                        triggerVibration()
                                                    }
                                                }
                                            },
                                            isError = patternError
                                        )
                                    }

                                    LockType.KNOCK -> {
                                        // Knock layout & length selectors
                                        Row(
                                            modifier = Modifier.padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            KnockLayout.entries.forEach { lay ->
                                                val isLay = newKnockLayout == lay
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isLay) GlassAccentCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f))
                                                        .border(1.dp, if (isLay) GlassAccentCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            newKnockLayout = lay
                                                            enteredKnock = emptyList()
                                                            isConfirmingKnock = false
                                                        }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(lay.displayName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.padding(bottom = 10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            KnockLength.entries.forEach { len ->
                                                val isLen = newKnockLength == len
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isLen) GlassAccentCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f))
                                                        .border(1.dp, if (isLen) GlassAccentCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            newKnockLength = len
                                                            enteredKnock = emptyList()
                                                            isConfirmingKnock = false
                                                        }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text("${len.length} Taps", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Text(
                                            text = if (!isConfirmingKnock) "Tap ${newKnockLength.length}-knock sequence" else "Repeat knock sequence to confirm",
                                            color = if (knockError) NeonRed else Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        KnockLockView(
                                            onKnockComplete = { sequence ->
                                                knockError = false
                                                if (!isConfirmingKnock) {
                                                    enteredKnock = sequence
                                                    isConfirmingKnock = true
                                                } else {
                                                    if (sequence == enteredKnock) {
                                                        repository.saveKnock(sequence, newKnockLayout, newKnockLength)
                                                        preferences.setLockType(LockType.KNOCK)
                                                        Toast.makeText(context, "New Knock sequence saved!", Toast.LENGTH_SHORT).show()
                                                        safeRecoverySuccess()
                                                    } else {
                                                        knockError = true
                                                        isConfirmingKnock = false
                                                        enteredKnock = emptyList()
                                                        triggerVibration()
                                                    }
                                                }
                                            },
                                            sequenceTargetLength = newKnockLength.length,
                                            layout = newKnockLayout,
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
    }
}
