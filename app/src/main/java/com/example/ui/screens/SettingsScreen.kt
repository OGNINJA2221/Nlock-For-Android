package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.PasswordRecoverySetupView
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.NLockApplication
import com.example.data.model.AppThemeMode
import com.example.data.model.LockType
import com.example.data.model.PinLength
import com.example.ui.components.BackgroundWrapper
import com.example.ui.components.GlassCard
import com.example.ui.theme.GlassAccentCyan
import com.example.ui.theme.GlassWhiteBorder
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onChangeLockMethod: () -> Unit
) {
    val context = LocalContext.current
    val repository = NLockApplication.instance.repository
    val preferences = NLockApplication.instance.preferences
    val securityManager = NLockApplication.instance.securityManager
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val currentTheme by preferences.themeFlow.collectAsState()
    val currentLockType by preferences.lockTypeFlow.collectAsState()

    var showStealthDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var backupPayloadString by remember { mutableStateOf("") }
    var restoreInputString by remember { mutableStateOf("") }

    var showRecoverySetupDialog by remember { mutableStateOf(false) }
    var isRecoveryConfigured by remember { mutableStateOf(preferences.isRecoveryConfigured) }

    var isAppLockEnabled by remember { mutableStateOf(preferences.isAppLockEnabled) }
    var isImmediateLock by remember { mutableStateOf(preferences.isImmediateLock) }
    var lockTimeoutSeconds by remember { mutableStateOf(preferences.lockTimeoutSeconds) }
    var showLockTimerDialog by remember { mutableStateOf(false) }
    var showCustomTimerDialog by remember { mutableStateOf(false) }

    var isHaptic by remember { mutableStateOf(preferences.isHapticFeedbackEnabled) }
    var isAnimation by remember { mutableStateOf(preferences.isAnimationEnabled) }

    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }
    var showExportSettingsDialog by remember { mutableStateOf(false) }
    var showImportSettingsDialog by remember { mutableStateOf(false) }
    var exportSettingsJsonString by remember { mutableStateOf("") }
    var importSettingsJsonString by remember { mutableStateOf("") }

    var isBiometric by remember { mutableStateOf(preferences.isBiometricEnabled) }
    var isFakeCrash by remember { mutableStateOf(preferences.isFakeCrashEnabled) }
    var isScreenshotProtected by remember { mutableStateOf(preferences.isScreenshotProtectionEnabled) }
    var isIntruderSelfie by remember { mutableStateOf(preferences.isIntruderSelfieEnabled) }
    var isStealth by remember { mutableStateOf(preferences.isStealthModeEnabled) }

    var hasCameraPermission by remember { mutableStateOf(securityManager.hasCameraPermission()) }
    var showCameraRationaleDialog by remember { mutableStateOf(false) }
    var showPermanentlyDeniedDialog by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            isIntruderSelfie = true
            preferences.isIntruderSelfieEnabled = true
        } else {
            val activity = context as? Activity
            val shouldShowRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
            if (!shouldShowRationale) {
                showPermanentlyDeniedDialog = true
            }
        }
    }

    val isRooted = remember { securityManager.isDeviceRooted() }

    BackgroundWrapper(themeMode = currentTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
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
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section: Lock Method & Credentials
                item {
                    SettingsSectionTitle("Lock Configuration")
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SettingsRow(
                                title = "Active Lock Type",
                                subtitle = currentLockType.displayName,
                                icon = Icons.Default.Lock,
                                onClick = onChangeLockMethod
                            )

                            SettingsRow(
                                title = "Change Code / Sequence",
                                subtitle = "Update your active credentials",
                                icon = Icons.Default.Pin,
                                onClick = onChangeLockMethod
                            )

                            SettingsRow(
                                title = "Password Recovery Questions",
                                subtitle = if (isRecoveryConfigured) "3 Questions Configured (Tap to update)" else "Not Configured - Tap to setup",
                                icon = Icons.Default.HelpOutline,
                                onClick = { showRecoverySetupDialog = true }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Biometric Unlock", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Fingerprint & Face recognition", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                                Switch(
                                    checked = isBiometric,
                                    onCheckedChange = {
                                        isBiometric = it
                                        preferences.isBiometricEnabled = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = GlassAccentCyan
                                    )
                                )
                            }
                        }
                    }
                }

                // Section: Lock Behavior
                item {
                    SettingsSectionTitle("Lock Behavior")
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // 1. App Lock Enabled
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "App Lock Enabled",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (isAppLockEnabled) "App locking is active" else "Locking disabled without uninstalling N Lock",
                                        color = if (isAppLockEnabled) NeonGreen else Color(0xFFFFB74D),
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = isAppLockEnabled,
                                    onCheckedChange = {
                                        isAppLockEnabled = it
                                        preferences.isAppLockEnabled = it
                                        Toast.makeText(
                                            context,
                                            if (it) "App lock resumed" else "App lock disabled",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = GlassAccentCyan,
                                        uncheckedThumbColor = Color.LightGray,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                            }

                            // 2. Lock Instantly When Leaving App
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Lock Instantly When Leaving App",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (isImmediateLock) "App relocks immediately upon leaving" else "Uses lock timer delay before relocking",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = isImmediateLock,
                                    onCheckedChange = {
                                        isImmediateLock = it
                                        preferences.isImmediateLock = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = GlassAccentCyan,
                                        uncheckedThumbColor = Color.LightGray,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                            }

                            // 3. Lock Timer
                            if (!isImmediateLock) {
                                SettingsRow(
                                    title = "Lock Timer",
                                    subtitle = "Relock after: ${formatLockTimeout(lockTimeoutSeconds)}",
                                    icon = Icons.Default.Timer,
                                    onClick = { showLockTimerDialog = true }
                                )
                            }
                        }
                    }
                }

                // Section: Security & Shields
                item {
                    SettingsSectionTitle("Security & Defense")
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Intruder Report (Toggle under Settings > Security)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "Intruder Report",
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (hasCameraPermission) NeonGreen.copy(alpha = 0.2f)
                                                        else NeonRed.copy(alpha = 0.2f)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (hasCameraPermission) "Camera Active" else "Permission Required",
                                                    color = if (hasCameraPermission) NeonGreen else NeonRed,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            "Capture front-camera photo after 3 failed attempts",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Switch(
                                        checked = isIntruderSelfie,
                                        onCheckedChange = { enabled ->
                                            if (enabled) {
                                                if (securityManager.hasCameraPermission()) {
                                                    hasCameraPermission = true
                                                    isIntruderSelfie = true
                                                    preferences.isIntruderSelfieEnabled = true
                                                } else {
                                                    showCameraRationaleDialog = true
                                                }
                                            } else {
                                                isIntruderSelfie = false
                                                preferences.isIntruderSelfieEnabled = false
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = GlassAccentCyan
                                        )
                                    )
                                }

                                // Warning if Camera permission is not granted
                                if (!hasCameraPermission) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(NeonRed.copy(alpha = 0.15f))
                                            .border(1.dp, NeonRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.WarningAmber,
                                                    contentDescription = "Warning",
                                                    tint = NeonRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Intruder Report requires Camera permission to capture photos of failed unlock attempts.",
                                                    color = Color.White.copy(alpha = 0.95f),
                                                    fontSize = 11.sp,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        val activity = context as? Activity
                                                        val shouldShow = activity != null &&
                                                            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
                                                        if (shouldShow) {
                                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                        } else {
                                                            // Open Android App Settings
                                                            val intent = Intent(
                                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                                Uri.fromParts("package", context.packageName, null)
                                                            ).apply {
                                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            }
                                                            context.startActivity(intent)
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GlassAccentCyan),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text("Grant Permission", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Fake Crash Screen
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Fake Crash Disguise", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Disguise lock as 'App Stopped' crash", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                                Switch(
                                    checked = isFakeCrash,
                                    onCheckedChange = {
                                        isFakeCrash = it
                                        preferences.isFakeCrashEnabled = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = GlassAccentCyan
                                    )
                                )
                            }

                            // Screenshot Protection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Screenshot Protection", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Block screen capture in lock screen", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                                Switch(
                                    checked = isScreenshotProtected,
                                    onCheckedChange = {
                                        isScreenshotProtected = it
                                        preferences.isScreenshotProtectionEnabled = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = GlassAccentCyan
                                    )
                                )
                            }

                            // Root Status Indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Root Integrity Status", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        if (isRooted) "Device has su binary or test-keys" else "Secure device environment",
                                        color = if (isRooted) NeonRed else NeonGreen,
                                        fontSize = 11.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isRooted) NeonRed.copy(alpha = 0.2f) else NeonGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        if (isRooted) "ROOTED" else "SAFE",
                                        color = if (isRooted) NeonRed else NeonGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Section: Stealth Mode
                item {
                    SettingsSectionTitle("Stealth Mode")
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Stealth Mode", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Hides app from launcher & recents", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                                Switch(
                                    checked = isStealth,
                                    onCheckedChange = {
                                        isStealth = it
                                        preferences.isStealthModeEnabled = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = GlassAccentCyan
                                    )
                                )
                            }

                            SettingsRow(
                                title = "Dialer Secret Code",
                                subtitle = preferences.stealthDialCode,
                                icon = Icons.Default.Dialpad,
                                onClick = { showStealthDialog = true }
                            )
                        }
                    }
                }

                // Section: Appearance
                item {
                    SettingsSectionTitle("Appearance & Theme")
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppThemeMode.entries.forEach { theme ->
                                val isSelected = currentTheme == theme
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) GlassAccentCyan.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable { preferences.setAppTheme(theme) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = if (isSelected) GlassAccentCyan else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = when (theme) {
                                                AppThemeMode.DEFAULT_GLASS_BLUE -> "Default Glass Blue (Original Asset)"
                                                AppThemeMode.DARK_GLASS -> "Dark Obsidian Glass"
                                                AppThemeMode.PURE_WHITE_GLASS -> "Pure White Crystal Glass"
                                                AppThemeMode.AUTO -> "Auto (Follow System)"
                                            },
                                            color = Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(GlassAccentCyan)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Haptic Feedback Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Haptic Feedback", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Vibrate on keypad and lock interaction", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                                Switch(
                                    checked = isHaptic,
                                    onCheckedChange = {
                                        isHaptic = it
                                        preferences.isHapticFeedbackEnabled = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = GlassAccentCyan
                                    )
                                )
                            }

                            // Fluid Animations Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("UI Motion & Animations", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Smooth glass transitions and pulse effects", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                                Switch(
                                    checked = isAnimation,
                                    onCheckedChange = {
                                        isAnimation = it
                                        preferences.isAnimationEnabled = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = GlassAccentCyan
                                    )
                                )
                            }
                        }
                    }
                }

                // Section: Backup & Restore
                item {
                    SettingsSectionTitle("Backup & Encryption")
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SettingsRow(
                                title = "Export Encrypted Backup",
                                subtitle = "AES-256 GCM encrypted token",
                                icon = Icons.Default.Backup,
                                onClick = {
                                    scope.launch {
                                        val backup = repository.exportEncryptedBackup()
                                        backupPayloadString = backup
                                        showBackupDialog = true
                                    }
                                }
                            )

                            SettingsRow(
                                title = "Restore from Encrypted Token",
                                subtitle = "Import security keys & configurations",
                                icon = Icons.Default.Refresh,
                                onClick = { showRestoreDialog = true }
                            )

                            SettingsRow(
                                title = "Export Settings (JSON)",
                                subtitle = "Shareable plaintext configuration",
                                icon = Icons.Default.Info,
                                onClick = {
                                    exportSettingsJsonString = preferences.exportSettingsJson()
                                    showExportSettingsDialog = true
                                }
                            )

                            SettingsRow(
                                title = "Import Settings (JSON)",
                                subtitle = "Restore configurations from JSON",
                                icon = Icons.Default.Refresh,
                                onClick = {
                                    importSettingsJsonString = ""
                                    showImportSettingsDialog = true
                                }
                            )
                        }
                    }
                }

                // Section: System Permissions & Services
                item {
                    SettingsSectionTitle("App Lock Engine & Permissions")
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SettingsRow(
                                title = "Battery Optimization Guidance",
                                subtitle = "Prevent Android OS from killing background lock monitors",
                                icon = Icons.Default.Bolt,
                                onClick = { showBatteryOptimizationDialog = true }
                            )

                            SettingsRow(
                                title = "Accessibility Service",
                                subtitle = "Primary engine to detect protected apps in real-time",
                                icon = Icons.Default.Security,
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                }
                            )

                            SettingsRow(
                                title = "Display Over Other Apps",
                                subtitle = if (securityManager.hasOverlayPermission()) "Permission Granted" else "Required to display lock overlay",
                                icon = Icons.Default.TouchApp,
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            )

                            SettingsRow(
                                title = "Usage Stats Access",
                                subtitle = if (securityManager.hasUsageStatsPermission()) "Permission Granted" else "Secondary monitoring engine",
                                icon = Icons.Default.Info,
                                onClick = {
                                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }

                // Section: About
                item {
                    SettingsSectionTitle("About N Lock")
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("N Lock v1.0.0", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Simple. Secure. Personal.", color = GlassAccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "Engineered with Android Keystore 256-bit AES-GCM encryption, CameraX intruder detection, and responsive Jetpack Compose glassmorphism UI.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Stealth Code Dialog
        if (showStealthDialog) {
            var codeInput by remember { mutableStateOf(preferences.stealthDialCode) }
            AlertDialog(
                onDismissRequest = { showStealthDialog = false },
                title = { Text("Stealth Dialer Code", color = Color.White) },
                text = {
                    Column {
                        Text(
                            "Type this code in your phone dialer app to open N Lock when stealth mode is enabled:",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GlassAccentCyan
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            preferences.stealthDialCode = codeInput
                            showStealthDialog = false
                            Toast.makeText(context, "Dialer code updated", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan, contentColor = Color.Black)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStealthDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF0D1B2A)
            )
        }

        // Backup Export Dialog
        if (showBackupDialog) {
            AlertDialog(
                onDismissRequest = { showBackupDialog = false },
                title = { Text("Encrypted Backup Token", color = Color.White) },
                text = {
                    Column {
                        Text(
                            "Your settings have been encrypted using Android Keystore AES-256-GCM:",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = backupPayloadString.take(120) + "...",
                                color = GlassAccentCyan,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("NLock Backup", backupPayloadString))
                            Toast.makeText(context, "Encrypted backup copied to clipboard", Toast.LENGTH_SHORT).show()
                            showBackupDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan, contentColor = Color.Black)
                    ) {
                        Text("Copy to Clipboard")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBackupDialog = false }) {
                        Text("Close", color = Color.White)
                    }
                },
                containerColor = Color(0xFF0D1B2A)
            )
        }

        // Restore Dialog
        if (showRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = false },
                title = { Text("Restore Settings", color = Color.White) },
                text = {
                    Column {
                        Text(
                            "Paste the encrypted backup token below:",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = restoreInputString,
                            onValueChange = { restoreInputString = it },
                            placeholder = { Text("Paste token here...", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp) },
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GlassAccentCyan
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            scope.launch {
                                val success = repository.importEncryptedBackup(restoreInputString)
                                if (success) {
                                    Toast.makeText(context, "Settings restored successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Invalid or corrupt backup token", Toast.LENGTH_SHORT).show()
                                }
                                showRestoreDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan, contentColor = Color.Black)
                    ) {
                        Text("Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        showRestoreDialog = false
                    }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF0D1B2A)
            )
        }

        // Camera Permission Explanation / Rationale Dialog
        if (showCameraRationaleDialog) {
            AlertDialog(
                onDismissRequest = { showCameraRationaleDialog = false },
                title = {
                    Text("Camera Permission Required", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "Intruder Report requires Camera permission to silently capture photos of unauthorized users after 3 failed unlock attempts.\n\nPhotos are stored securely on your device only and are never uploaded or shared.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showCameraRationaleDialog = false
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlassAccentCyan,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Grant Permission")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCameraRationaleDialog = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF0D1B2A)
            )
        }

        // Camera Permission Permanently Denied Dialog
        if (showPermanentlyDeniedDialog) {
            AlertDialog(
                onDismissRequest = { showPermanentlyDeniedDialog = false },
                title = {
                    Text("Camera Access Blocked", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "Camera permission was permanently denied. Android requires you to enable it manually from App Settings.\n\nPlease open App Settings, tap Permissions, and allow Camera access for N Lock.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermanentlyDeniedDialog = false
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlassAccentCyan,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermanentlyDeniedDialog = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF0D1B2A)
            )
        }

        if (showRecoverySetupDialog) {
            Dialog(
                onDismissRequest = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    showRecoverySetupDialog = false
                },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A))
                        .imePadding()
                        .padding(16.dp)
                ) {
                    PasswordRecoverySetupView(
                        initialQ1 = preferences.recoveryQuestion1 ?: "",
                        initialQ2 = preferences.recoveryQuestion2 ?: "",
                        initialQ3 = preferences.recoveryQuestion3 ?: "",
                        onSaveRecovery = { q1, a1, q2, a2, q3, a3 ->
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            repository.saveRecoverySetup(q1, a1, q2, a2, q3, a3)
                            isRecoveryConfigured = true
                            showRecoverySetupDialog = false
                            Toast.makeText(context, "Recovery setup updated successfully", Toast.LENGTH_SHORT).show()
                        }
                    )
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            showRecoverySetupDialog = false
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }

        // Lock Timer Presets Dialog
        if (showLockTimerDialog) {
            val presetOptions = listOf(
                Pair("10 seconds", 10),
                Pair("20 seconds", 20),
                Pair("30 seconds", 30),
                Pair("40 seconds", 40),
                Pair("50 seconds", 50),
                Pair("60 seconds (1 min)", 60)
            )

            AlertDialog(
                onDismissRequest = { showLockTimerDialog = false },
                title = {
                    Text("Select Lock Timer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "App remains unlocked for this duration after you exit before relocking.",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        presetOptions.forEach { (label, secs) ->
                            val isCurrent = lockTimeoutSeconds == secs
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrent) GlassAccentCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
                                    .clickable {
                                        lockTimeoutSeconds = secs
                                        preferences.lockTimeoutSeconds = secs
                                        showLockTimerDialog = false
                                        Toast.makeText(context, "Lock timer set to $label", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(GlassAccentCyan)
                                    )
                                }
                            }
                        }

                        // Custom Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .clickable {
                                    showLockTimerDialog = false
                                    showCustomTimerDialog = true
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Custom Timer (1s - 24h)...",
                                color = GlassAccentCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = GlassAccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showLockTimerDialog = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF0D1B2A)
            )
        }

        // Custom Lock Timer Dialog
        if (showCustomTimerDialog) {
            var inputVal by remember { mutableStateOf("5") }
            var selectedUnit by remember { mutableStateOf("Minutes") } // "Seconds", "Minutes", "Hours"
            val units = listOf("Seconds", "Minutes", "Hours")

            AlertDialog(
                onDismissRequest = { showCustomTimerDialog = false },
                title = {
                    Text("Custom Lock Timer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Column {
                        Text(
                            "Enter the timeout duration before protected apps relock (1 second to 24 hours):",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = inputVal,
                            onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 6) inputVal = it },
                            label = { Text("Duration", color = Color.White.copy(alpha = 0.7f)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GlassAccentCyan
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Unit Selector Tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            units.forEach { unit ->
                                val isSelected = selectedUnit == unit
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) GlassAccentCyan else Color.White.copy(alpha = 0.12f))
                                        .clickable { selectedUnit = unit }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = unit,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        val parsedNumber = inputVal.toIntOrNull() ?: 0
                        val computedSeconds = when (selectedUnit) {
                            "Seconds" -> parsedNumber
                            "Minutes" -> parsedNumber * 60
                            "Hours" -> parsedNumber * 3600
                            else -> parsedNumber
                        }.coerceIn(1, 86400) // Max 24 hours

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Effective Timeout: ${formatLockTimeout(computedSeconds)}",
                            color = GlassAccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val parsed = inputVal.toIntOrNull() ?: 1
                            val computed = when (selectedUnit) {
                                "Seconds" -> parsed
                                "Minutes" -> parsed * 60
                                "Hours" -> parsed * 3600
                                else -> parsed
                            }.coerceIn(1, 86400)

                            preferences.lockTimeoutSeconds = computed
                            lockTimeoutSeconds = computed
                            showCustomTimerDialog = false
                            Toast.makeText(context, "Lock timer updated to ${formatLockTimeout(computed)}", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan, contentColor = Color.Black)
                    ) {
                        Text("Save Timer", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomTimerDialog = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF0D1B2A)
            )
        }

        // Battery Optimization Guidance Dialog
        if (showBatteryOptimizationDialog) {
            AlertDialog(
                onDismissRequest = { showBatteryOptimizationDialog = false },
                title = {
                    Text("Battery Optimization Guidance", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "To ensure N Lock reliably detects and locks protected apps without interruption:",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                        Text(
                            "1. Select 'Unrestricted' or 'Don't optimize' in Android Battery settings.",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                        Text(
                            "2. Allow background activity and autostart (on Xiaomi, Samsung, OnePlus, Oppo).",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                        Text(
                            "3. Lock N Lock in your Recent Apps list to prevent task killer cleanup.",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showBatteryOptimizationDialog = false
                            try {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan, contentColor = Color.Black)
                    ) {
                        Text("Open Battery Settings", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatteryOptimizationDialog = false }) {
                        Text("Got It", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF0D1B2A)
            )
        }

        // Export Settings (JSON) Dialog
        if (showExportSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showExportSettingsDialog = false },
                title = {
                    Text("Export Settings (JSON)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Column {
                        Text(
                            "Copy this JSON payload to backup or migrate your N Lock settings:",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = exportSettingsJsonString,
                                color = GlassAccentCyan,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("NLock_Settings", exportSettingsJsonString))
                            Toast.makeText(context, "Settings JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                            showExportSettingsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan, contentColor = Color.Black)
                    ) {
                        Text("Copy JSON", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportSettingsDialog = false }) {
                        Text("Close", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF0D1B2A)
            )
        }

        // Import Settings (JSON) Dialog
        if (showImportSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showImportSettingsDialog = false },
                title = {
                    Text("Import Settings (JSON)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Column {
                        Text(
                            "Paste an exported N Lock JSON settings payload to restore configuration:",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = importSettingsJsonString,
                            onValueChange = { importSettingsJsonString = it },
                            placeholder = { Text("Paste JSON here...", color = Color.White.copy(alpha = 0.4f)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GlassAccentCyan
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val success = preferences.importSettingsJson(importSettingsJsonString)
                            if (success) {
                                isAppLockEnabled = preferences.isAppLockEnabled
                                isImmediateLock = preferences.isImmediateLock
                                lockTimeoutSeconds = preferences.lockTimeoutSeconds
                                isHaptic = preferences.isHapticFeedbackEnabled
                                isAnimation = preferences.isAnimationEnabled
                                isBiometric = preferences.isBiometricEnabled
                                showImportSettingsDialog = false
                                Toast.makeText(context, "Settings imported successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Invalid JSON settings format", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan, contentColor = Color.Black)
                    ) {
                        Text("Restore", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportSettingsDialog = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF0D1B2A)
            )
        }
    }
}

private fun formatLockTimeout(seconds: Int): String {
    return when {
        seconds < 60 -> "$seconds seconds"
        seconds % 3600 == 0 -> {
            val h = seconds / 3600
            "$h ${if (h == 1) "hour" else "hours"}"
        }
        seconds % 60 == 0 -> {
            val m = seconds / 60
            "$m ${if (m == 1) "minute" else "minutes"}"
        }
        else -> {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            if (h > 0) "${h}h ${m}m ${s}s" else "${m}m ${s}s"
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = GlassAccentCyan,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
    }
}
