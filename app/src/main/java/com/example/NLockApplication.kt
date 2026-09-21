package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.db.AppDatabase
import com.example.data.pref.AppPreferences
import com.example.data.repository.AppLockRepository
import com.example.security.CryptoManager
import com.example.security.SecurityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NLockApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: AppDatabase
        private set
    lateinit var preferences: AppPreferences
        private set
    lateinit var cryptoManager: CryptoManager
        private set
    lateinit var securityManager: SecurityManager
        private set
    lateinit var repository: AppLockRepository
        private set
    lateinit var screenTimeManager: com.example.security.ScreenTimeManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        preferences = AppPreferences(this)
        cryptoManager = CryptoManager()
        securityManager = SecurityManager(this)
        repository = AppLockRepository(this, database, preferences, cryptoManager)
        screenTimeManager = com.example.security.ScreenTimeManager(this)

        createNotificationChannels()

        // Sync apps in background
        applicationScope.launch {
            try {
                repository.syncInstalledApps()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_SERVICE,
                "N Lock Active Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors and locks protected applications"
                setShowBadge(false)
            }
            val intruderChannel = NotificationChannel(
                CHANNEL_INTRUDER,
                "Intruder Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when unauthorized access attempts occur"
            }

            val screenTimeChannel = NotificationChannel(
                CHANNEL_SCREEN_TIME,
                "Screen Time Manager",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when screen time goals and milestones are reached"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(intruderChannel)
            notificationManager.createNotificationChannel(screenTimeChannel)
        }
    }

    companion object {
        const val CHANNEL_SERVICE = "nlock_service_channel"
        const val CHANNEL_INTRUDER = "nlock_intruder_channel"
        const val CHANNEL_SCREEN_TIME = "nlock_screentime_channel"

        lateinit var instance: NLockApplication
            private set
    }
}
