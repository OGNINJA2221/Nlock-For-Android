package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import coil.compose.AsyncImage
import com.example.NLockApplication
import com.example.data.model.IntruderLogEntity
import com.example.ui.components.BackgroundWrapper
import com.example.ui.components.GlassCard
import com.example.ui.theme.GlassAccentCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IntruderReportScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = NLockApplication.instance.repository
    val preferences = NLockApplication.instance.preferences
    val securityManager = NLockApplication.instance.securityManager
    val scope = rememberCoroutineScope()

    val intruderLogs by repository.intruderLogs.collectAsState(initial = emptyList())
    val themeMode by preferences.themeFlow.collectAsState()

    var hasCameraPermission by remember { mutableStateOf(securityManager.hasCameraPermission()) }
    var zoomedLog by remember { mutableStateOf<IntruderLogEntity?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            val activity = context as? Activity
            val shouldShow = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
            if (!shouldShow) {
                // Permanently denied - redirect to Settings
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    BackgroundWrapper(themeMode = themeMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Bar
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
                        text = "Intruder Reports",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${intruderLogs.size} incidents recorded",
                        color = if (intruderLogs.isNotEmpty()) NeonRed else GlassAccentCyan,
                        fontSize = 12.sp
                    )
                }

                if (intruderLogs.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                repository.clearAllIntruderLogs()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear All",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Camera Permission Warning Banner if not granted
            if (!hasCameraPermission) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = NeonRed.copy(alpha = 0.18f),
                    borderColor = NeonRed.copy(alpha = 0.6f)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Camera Permission Warning",
                                tint = NeonRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Intruder Report requires Camera permission to capture photos of failed unlock attempts.",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    val activity = context as? Activity
                                    val shouldShow = activity != null &&
                                        ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
                                    if (shouldShow) {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    } else {
                                        val intent = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null)
                                        ).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GlassAccentCyan,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Grant Permission", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Intruder Status Banner
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = if (intruderLogs.isNotEmpty()) NeonRed.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.10f),
                borderColor = if (intruderLogs.isNotEmpty()) NeonRed.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (intruderLogs.isNotEmpty()) NeonRed.copy(alpha = 0.25f) else GlassAccentCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = if (intruderLogs.isNotEmpty()) NeonRed else GlassAccentCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (intruderLogs.isNotEmpty()) "Unauthorized Access Logged" else "No Intruders Detected",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Auto-captures selfie after ${preferences.intruderThreshold} consecutive failed unlock tries.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (intruderLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = GlassAccentCyan,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Intruder Photos",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "When someone enters an incorrect PIN, Pattern, or Knock 3 times, their photo and attempt details will appear here.",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(intruderLogs, key = { it.id }) { log ->
                        IntruderLogCard(
                            log = log,
                            onPhotoClick = { zoomedLog = log },
                            onDelete = {
                                scope.launch {
                                    repository.deleteIntruderLog(log.id)
                                }
                            },
                            onShare = {
                                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "N Lock Security Alert")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Security Alert: An unauthorized user attempted ${log.failedAttempts} failed unlock attempts on ${log.targetAppName} (${log.targetPackageName}) using ${log.lockTypeUsed} at $dateStr."
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Security Evidence"))
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Zoomed Photo Dialog with Detailed Incident Info
        zoomedLog?.let { log ->
            val dateFormatted = remember(log.timestamp) {
                SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(Date(log.timestamp))
            }
            val timeFormatted = remember(log.timestamp) {
                SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(log.timestamp))
            }

            Dialog(onDismissRequest = { zoomedLog = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0D1B2A))
                        .border(1.dp, GlassAccentCyan.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Intruder Evidence",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (log.photoPath != null && File(log.photoPath).exists()) {
                            AsyncImage(
                                model = File(log.photoPath),
                                contentDescription = "Intruder Selfie",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, NeonRed, RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(NeonRed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = NeonRed,
                                        modifier = Modifier.size(50.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("No Photo Captured", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Incident Details
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Target App: ${log.targetAppName}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            if (log.targetPackageName.isNotEmpty()) {
                                Text("Package: ${log.targetPackageName}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                            Text("Date: $dateFormatted", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                            Text("Time: $timeFormatted", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                            Text("Method: ${log.lockTypeUsed}", color = GlassAccentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Failed Attempts: ${log.failedAttempts}", color = NeonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { zoomedLog = null },
                            colors = ButtonDefaults.buttonColors(containerColor = GlassAccentCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dismiss", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntruderLogCard(
    log: IntruderLogEntity,
    onPhotoClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val dateStr = remember(log.timestamp) {
        SimpleDateFormat("EEE, MMM dd • hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo Thumbnail
            if (log.photoPath != null && File(log.photoPath).exists()) {
                AsyncImage(
                    model = File(log.photoPath),
                    contentDescription = "Intruder Thumbnail",
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, NeonRed, RoundedCornerShape(16.dp))
                        .clickable { onPhotoClick() },
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NeonRed.copy(alpha = 0.2f))
                        .border(1.dp, NeonRed, RoundedCornerShape(16.dp))
                        .clickable { onPhotoClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = NeonRed,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPhotoClick() }
            ) {
                Text(
                    text = "Breach: ${log.targetAppName}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (log.targetPackageName.isNotEmpty()) {
                    Text(
                        text = log.targetPackageName,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${log.failedAttempts} failed attempts • ${log.lockTypeUsed}",
                    color = NeonRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateStr,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }

            Row {
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
