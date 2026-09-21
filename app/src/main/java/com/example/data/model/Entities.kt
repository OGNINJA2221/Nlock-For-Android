package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locked_apps")
data class LockedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isLocked: Boolean = true,
    val category: String = AppCategory.OTHER.name,
    val isRecommended: Boolean = false,
    val fakeCrashEnabled: Boolean = false,
    val lockCount: Int = 0,
    val lastUnlockedTimestamp: Long = 0L
)

@Entity(tableName = "intruder_logs")
data class IntruderLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val targetAppName: String,
    val targetPackageName: String = "",
    val failedAttempts: Int = 3,
    val photoPath: String? = null,
    val lockTypeUsed: String = LockType.PIN.name
)

@Entity(tableName = "security_logs")
data class SecurityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val description: String,
    val isSuccess: Boolean = true
)
