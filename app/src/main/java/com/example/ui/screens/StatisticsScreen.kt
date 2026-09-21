package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NLockApplication
import com.example.ui.components.BackgroundWrapper
import com.example.ui.components.GlassCard
import com.example.ui.theme.GlassAccentCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed

@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit
) {
    val repository = NLockApplication.instance.repository
    val preferences = NLockApplication.instance.preferences
    val securityManager = NLockApplication.instance.securityManager

    val lockedCount by repository.lockedCount.collectAsState(initial = 0)
    val intruderCount by repository.intruderLogCount.collectAsState(initial = 0)
    val successCount by repository.successfulUnlockCount.collectAsState(initial = 0)
    val failedCount by repository.failedUnlockCount.collectAsState(initial = 0)
    val themeMode by preferences.themeFlow.collectAsState()

    val hasUsage = securityManager.hasUsageStatsPermission()
    val hasOverlay = securityManager.hasOverlayPermission()

    val securityScore = securityManager.calculateSecurityScore(
        isLockConfigured = preferences.isLockConfigured,
        isIntruderSelfieEnabled = preferences.isIntruderSelfieEnabled,
        isStealthEnabled = preferences.isStealthModeEnabled,
        isFakeCrashEnabled = preferences.isFakeCrashEnabled,
        isScreenshotProtected = preferences.isScreenshotProtectionEnabled,
        isBiometricEnabled = preferences.isBiometricEnabled,
        isUsageStatsGranted = hasUsage,
        isOverlayGranted = hasOverlay
    )

    BackgroundWrapper(themeMode = themeMode) {
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
                    text = "Security Statistics",
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
                // Big Security Score Overview Card
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Overall Security Index",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (securityScore >= 80) "Optimal Defense" else "Action Needed",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Based on active defense modules and system protection shields.",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(
                                modifier = Modifier.size(86.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { securityScore / 100f },
                                    modifier = Modifier.fillMaxSize(),
                                    color = if (securityScore >= 80) GlassAccentCyan else Color(0xFFFFAB00),
                                    trackColor = Color.White.copy(alpha = 0.15f),
                                    strokeWidth = 7.dp
                                )
                                Text(
                                    text = "$securityScore%",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }
                }

                // 4 Metric Counters Grid
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatMetricCard(
                            title = "Protected Apps",
                            value = "$lockedCount",
                            icon = Icons.Default.Apps,
                            iconColor = GlassAccentCyan,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            title = "Intruder Alerts",
                            value = "$intruderCount",
                            icon = Icons.Default.CameraAlt,
                            iconColor = if (intruderCount > 0) NeonRed else GlassAccentCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatMetricCard(
                            title = "Successful Unlocks",
                            value = "$successCount",
                            icon = Icons.Default.CheckCircle,
                            iconColor = NeonGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            title = "Failed Attempts",
                            value = "$failedCount",
                            icon = Icons.Default.Warning,
                            iconColor = NeonRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Security Shield Breakdown List
                item {
                    Text(
                        text = "Protection Checklist",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                val checklist = listOf(
                    Triple("Lock Setup Configured", preferences.isLockConfigured, 25),
                    Triple("Usage Access Permission", hasUsage, 15),
                    Triple("Display Over Apps Permission", hasOverlay, 10),
                    Triple("Intruder Selfie Active", preferences.isIntruderSelfieEnabled, 15),
                    Triple("Screenshot Protection (FLAG_SECURE)", preferences.isScreenshotProtectionEnabled, 10),
                    Triple("Biometric Authentication", preferences.isBiometricEnabled, 10),
                    Triple("Fake Crash Disguise", preferences.isFakeCrashEnabled, 8),
                    Triple("Stealth Mode Enabled", preferences.isStealthModeEnabled, 7)
                )

                items(checklist.size) { index ->
                    val (label, enabled, points) = checklist[index]
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (enabled) NeonGreen.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (enabled) Icons.Default.CheckCircle else Icons.Default.Security,
                                        contentDescription = null,
                                        tint = if (enabled) NeonGreen else Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = if (enabled) "+$points pts" else "0 pts",
                                color = if (enabled) NeonGreen else Color.White.copy(alpha = 0.4f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Text(text = title, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
        }
    }
}
