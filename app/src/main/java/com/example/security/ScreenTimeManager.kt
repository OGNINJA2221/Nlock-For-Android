package com.example.security

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.NLockApplication
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val foregroundTimeMillis: Long,
    val formattedTime: String,
    val percentageOfTotal: Float,
    val icon: Drawable? = null
)

data class ScreenTimeSummary(
    val weeklyGoalMinutes: Int,
    val dailyAverageGoalMinutes: Float,
    val todayUsageMillis: Long,
    val todayUsageMinutes: Long,
    val currentWeekUsageMillis: Long,
    val currentWeekUsageMinutes: Long,
    val remainingMinutes: Long,
    val progressFraction: Float,
    val appBreakdown: List<AppUsageInfo>,
    val hasPermission: Boolean
)

class ScreenTimeManager(private val context: Context) {

    private val securityManager = NLockApplication.instance.securityManager
    private val preferences = NLockApplication.instance.preferences

    suspend fun getScreenTimeSummary(): ScreenTimeSummary = withContext(Dispatchers.IO) {
        val hasPerm = securityManager.hasUsageStatsPermission()
        val weeklyGoalMinutes = preferences.weeklyScreenTimeGoalMinutes
        val dailyAverageGoalMinutes = weeklyGoalMinutes / 7f

        val now = System.currentTimeMillis()

        // Today start
        val calToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = calToday.timeInMillis

        // Week start (Monday)
        val calWeek = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var weekStart = calWeek.timeInMillis
        if (weekStart > now) {
            weekStart -= 7L * 24 * 60 * 60 * 1000L
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val pm = context.packageManager

        var todayUsageMillis = 0L
        var weekUsageMillis = 0L
        val appList = mutableListOf<AppUsageInfo>()

        if (hasPerm && usageStatsManager != null) {
            val weekStatsMap = usageStatsManager.queryAndAggregateUsageStats(weekStart, now)
            val todayStatsMap = usageStatsManager.queryAndAggregateUsageStats(todayStart, now)

            val aggregatedApps = mutableListOf<Triple<String, String, Long>>()

            for ((pkg, stats) in weekStatsMap) {
                if (!shouldIncludeInScreenTime(pkg)) continue
                val time = stats.totalTimeInForeground
                if (time > 60_000L) { // At least 1 minute
                    val appName = try {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        pkg.substringAfterLast('.')
                    }
                    aggregatedApps.add(Triple(pkg, appName, time))
                }
            }

            weekUsageMillis = aggregatedApps.sumOf { it.third }
            todayUsageMillis = todayStatsMap.values
                .filter { shouldIncludeInScreenTime(it.packageName) }
                .sumOf { it.totalTimeInForeground }

            // Sort apps by usage descending
            aggregatedApps.sortByDescending { it.third }

            val totalForPercentages = if (weekUsageMillis > 0) weekUsageMillis.toFloat() else 1f
            for ((pkg, name, time) in aggregatedApps.take(15)) {
                val icon = try { pm.getApplicationIcon(pkg) } catch (e: Exception) { null }
                appList.add(
                    AppUsageInfo(
                        packageName = pkg,
                        appName = name,
                        foregroundTimeMillis = time,
                        formattedTime = formatDuration(time),
                        percentageOfTotal = (time.toFloat() / totalForPercentages).coerceIn(0f, 1f),
                        icon = icon
                    )
                )
            }
        }

        val weekUsageMinutes = weekUsageMillis / (60 * 1000L)
        val todayUsageMinutes = todayUsageMillis / (60 * 1000L)
        val remainingMinutes = maxOf(0L, weeklyGoalMinutes.toLong() - weekUsageMinutes)
        val progressFraction = if (weeklyGoalMinutes > 0) {
            weekUsageMinutes.toFloat() / weeklyGoalMinutes.toFloat()
        } else {
            0f
        }

        // Check and send milestone notifications if enabled
        checkAndTriggerGoalNotifications(progressFraction, weekUsageMinutes, weeklyGoalMinutes)

        ScreenTimeSummary(
            weeklyGoalMinutes = weeklyGoalMinutes,
            dailyAverageGoalMinutes = dailyAverageGoalMinutes,
            todayUsageMillis = todayUsageMillis,
            todayUsageMinutes = todayUsageMinutes,
            currentWeekUsageMillis = weekUsageMillis,
            currentWeekUsageMinutes = weekUsageMinutes,
            remainingMinutes = remainingMinutes,
            progressFraction = progressFraction,
            appBreakdown = appList,
            hasPermission = hasPerm
        )
    }

    private fun checkAndTriggerGoalNotifications(
        progress: Float,
        currentMinutes: Long,
        goalMinutes: Int
    ) {
        if (!preferences.isScreenTimeGoalEnabled) return

        val currentWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)
        if (preferences.lastNotifiedWeekNumber != currentWeek) {
            preferences.lastNotifiedWeekNumber = currentWeek
            preferences.lastNotifiedGoalMilestone = 0
        }

        val lastMilestone = preferences.lastNotifiedGoalMilestone

        when {
            progress >= 1.05f && lastMilestone < 4 -> {
                preferences.lastNotifiedGoalMilestone = 4
                sendScreenTimeNotification(
                    id = 104,
                    title = "Screen Time Goal Exceeded!",
                    message = "You have used ${formatMinutes(currentMinutes)}, exceeding your $goalMinutes min weekly goal."
                )
            }
            progress >= 1.0f && lastMilestone < 3 -> {
                preferences.lastNotifiedGoalMilestone = 3
                sendScreenTimeNotification(
                    id = 103,
                    title = "Weekly Goal Reached!",
                    message = "You have reached 100% of your weekly screen time goal (${formatMinutes(currentMinutes)})."
                )
            }
            progress >= 0.80f && lastMilestone < 2 -> {
                preferences.lastNotifiedGoalMilestone = 2
                sendScreenTimeNotification(
                    id = 102,
                    title = "80% Screen Time Alert",
                    message = "You have reached 80% of your weekly screen-time limit. Stay mindful!"
                )
            }
            progress >= 0.50f && lastMilestone < 1 -> {
                preferences.lastNotifiedGoalMilestone = 1
                sendScreenTimeNotification(
                    id = 101,
                    title = "50% Screen Time Reached",
                    message = "You are halfway through your weekly screen-time budget."
                )
            }
        }
    }

    private fun sendScreenTimeNotification(id: Int, title: String, message: String) {
        try {
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, NLockApplication.CHANNEL_SCREEN_TIME)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(id, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shouldIncludeInScreenTime(packageName: String): Boolean {
        if (packageName == context.packageName) return false
        if (packageName == "com.android.systemui") return false
        if (packageName == "android") return false
        if (packageName.contains("launcher")) return false
        if (packageName.contains("inputmethod")) return false
        return true
    }

    fun formatDuration(millis: Long): String {
        val totalMinutes = millis / (60 * 1000L)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    fun formatMinutes(minutes: Long): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }
}
