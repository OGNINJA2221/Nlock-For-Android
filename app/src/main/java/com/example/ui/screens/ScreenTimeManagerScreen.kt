package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.NLockApplication
import com.example.security.AppUsageInfo
import com.example.security.ScreenTimeSummary
import com.example.ui.components.BackgroundWrapper
import com.example.ui.components.GlassCard
import com.example.ui.theme.GlassAccentCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ScreenTimeManagerScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val preferences = NLockApplication.instance.preferences
    val screenTimeManager = NLockApplication.instance.screenTimeManager
    val scope = rememberCoroutineScope()

    val themeMode by preferences.themeFlow.collectAsState()
    var summary by remember { mutableStateOf<ScreenTimeSummary?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showGoalDialog by remember { mutableStateOf(false) }

    fun refreshStats() {
        scope.launch {
            isLoading = true
            summary = screenTimeManager.getScreenTimeSummary()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshStats()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshStats()
    }

    BackgroundWrapper(themeMode = themeMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Header Bar
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Screen Time Manager",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Weekly goals & application usage limits",
                        color = GlassAccentCyan,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = { showGoalDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Goal",
                        tint = GlassAccentCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading && summary == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GlassAccentCyan)
                }
            } else {
                val currentSummary = summary
                if (currentSummary != null) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Permission alert if needed
                        if (!currentSummary.hasPermission) {
                            item {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color(0xFFFFB74D),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Usage Access Required",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "To track real-time app screen-time accurately, please grant Android Usage Access.",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(GlassAccentCyan)
                                                .clickable {
                                                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                                    context.startActivity(intent)
                                                }
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = "Grant Usage Permission",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Circular Weekly Progress Ring Card
                        item {
                            val progressFraction = currentSummary.progressFraction
                            val ringColor = when {
                                progressFraction >= 1.0f -> NeonRed
                                progressFraction >= 0.80f -> Color(0xFFFFB74D) // Yellow / Orange near goal
                                else -> NeonGreen
                            }

                            val statusText = when {
                                progressFraction >= 1.0f -> "Goal Exceeded"
                                progressFraction >= 0.80f -> "Near Goal (80%+)"
                                else -> "Below Goal"
                            }

                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Weekly Progress",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                text = "Status: $statusText",
                                                color = ringColor,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(ringColor.copy(alpha = 0.2f))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${(progressFraction * 100).roundToInt()}%",
                                                color = ringColor,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Progress Ring
                                    Box(
                                        modifier = Modifier.size(150.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { progressFraction.coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxSize(),
                                            color = ringColor,
                                            trackColor = Color.White.copy(alpha = 0.12f),
                                            strokeWidth = 12.dp
                                        )
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = screenTimeManager.formatMinutes(currentSummary.currentWeekUsageMinutes),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 22.sp
                                            )
                                            Text(
                                                text = "of ${screenTimeManager.formatMinutes(currentSummary.weeklyGoalMinutes.toLong())}",
                                                color = Color.White.copy(alpha = 0.65f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // Metric breakdown grid 2x2
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Weekly Goal", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                            Text(
                                                text = screenTimeManager.formatMinutes(currentSummary.weeklyGoalMinutes.toLong()),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Daily Average Goal", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                            val dailyMins = currentSummary.dailyAverageGoalMinutes
                                            val dailyFormatted = if (dailyMins >= 60f) {
                                                String.format("%.1f hrs", dailyMins / 60f)
                                            } else {
                                                String.format("%.1f mins", dailyMins)
                                            }
                                            Text(
                                                text = dailyFormatted,
                                                color = GlassAccentCyan,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Today's Usage", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                            Text(
                                                text = screenTimeManager.formatMinutes(currentSummary.todayUsageMinutes),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Remaining Time", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                            Text(
                                                text = if (currentSummary.remainingMinutes > 0) {
                                                    screenTimeManager.formatMinutes(currentSummary.remainingMinutes)
                                                } else {
                                                    "Limit reached"
                                                },
                                                color = if (currentSummary.remainingMinutes > 0) NeonGreen else NeonRed,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Milestone Notifications Card
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.NotificationsActive,
                                                contentDescription = null,
                                                tint = GlassAccentCyan,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Milestone Notifications",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = "50%, 80%, Goal reached, and Exceeded alerts",
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = preferences.isScreenTimeGoalEnabled,
                                            onCheckedChange = {
                                                preferences.isScreenTimeGoalEnabled = it
                                                refreshStats()
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = GlassAccentCyan,
                                                uncheckedThumbColor = Color.LightGray,
                                                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Milestone checklist
                                    val currentProgress = currentSummary.progressFraction
                                    MilestoneBadge(label = "50% Goal Reached", isPassed = currentProgress >= 0.50f)
                                    MilestoneBadge(label = "80% Warning (Near Goal)", isPassed = currentProgress >= 0.80f)
                                    MilestoneBadge(label = "100% Goal Reached", isPassed = currentProgress >= 1.0f)
                                    MilestoneBadge(label = "Goal Exceeded", isPassed = currentProgress >= 1.05f)
                                }
                            }
                        }

                        // App Breakdown Section Title
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Usage Breakdown by App",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${currentSummary.appBreakdown.size} Apps",
                                    color = GlassAccentCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // App Breakdown Items
                        if (currentSummary.appBreakdown.isEmpty()) {
                            item {
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassBottom,
                                            contentDescription = null,
                                            tint = GlassAccentCyan.copy(alpha = 0.6f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No App Usage Recorded Yet",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "As you use applications on your device, real foreground usage will appear here automatically.",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(currentSummary.appBreakdown) { app ->
                                AppUsageRow(app = app)
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    // Set Weekly Goal Dialog
    if (showGoalDialog) {
        var sliderHours by remember { mutableFloatStateOf(preferences.weeklyScreenTimeGoalMinutes / 60f) }

        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = {
                Text(
                    text = "Set Weekly Screen-Time Goal",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Choose your total target screen-time budget for the entire week.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val hours = sliderHours.roundToInt()
                    val totalMinutes = hours * 60
                    val dailyAverage = totalMinutes / 7f
                    val dailyFormatted = if (dailyAverage >= 60f) {
                        String.format("%.1f hrs/day", dailyAverage / 60f)
                    } else {
                        String.format("%.1f mins/day", dailyAverage)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$hours Hours / Week",
                            color = GlassAccentCyan,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "($dailyFormatted)",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = sliderHours,
                        onValueChange = { sliderHours = it },
                        valueRange = 1f..40f,
                        steps = 38,
                        colors = SliderDefaults.colors(
                            thumbColor = GlassAccentCyan,
                            activeTrackColor = GlassAccentCyan,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Example: 3 Hours = 25.7 mins average daily goal",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val chosenMinutes = (sliderHours.roundToInt()) * 60
                        preferences.weeklyScreenTimeGoalMinutes = chosenMinutes
                        showGoalDialog = false
                        refreshStats()
                    }
                ) {
                    Text("Save Goal", color = GlassAccentCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF0C192E)
        )
    }
}

@Composable
private fun MilestoneBadge(label: String, isPassed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isPassed) Icons.Default.CheckCircle else Icons.Default.HourglassBottom,
                contentDescription = null,
                tint = if (isPassed) NeonGreen else Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = if (isPassed) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        }
        Text(
            text = if (isPassed) "Triggered" else "Pending",
            color = if (isPassed) NeonGreen else Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AppUsageRow(app: AppUsageInfo) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon or generic icon
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon.toBitmap(80, 80).asImageBitmap(),
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GlassAccentCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = GlassAccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = app.packageName,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                Text(
                    text = app.formattedTime,
                    color = GlassAccentCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Percentage bar
            LinearProgressIndicator(
                progress = { app.percentageOfTotal },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = GlassAccentCyan,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}
