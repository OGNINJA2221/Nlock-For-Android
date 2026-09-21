package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.data.db.AppDatabase
import com.example.data.model.AppCategory
import com.example.data.model.IntruderLogEntity
import com.example.data.model.KnockLayout
import com.example.data.model.KnockLength
import com.example.data.model.LockedAppEntity
import com.example.data.model.PinLength
import com.example.data.model.SecurityLogEntity
import com.example.data.pref.AppPreferences
import com.example.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AppLockRepository(
    private val context: Context,
    private val database: AppDatabase,
    val preferences: AppPreferences,
    val cryptoManager: CryptoManager
) {
    private val lockedAppDao = database.lockedAppDao()
    private val intruderLogDao = database.intruderLogDao()
    private val securityLogDao = database.securityLogDao()

    val allLockedApps: Flow<List<LockedAppEntity>> = lockedAppDao.getAllLockedApps()
    val activeLockedApps: Flow<List<LockedAppEntity>> = lockedAppDao.getActiveLockedApps()
    val lockedCount: Flow<Int> = lockedAppDao.getLockedCount()
    val intruderLogs: Flow<List<IntruderLogEntity>> = intruderLogDao.getAllLogs()
    val intruderLogCount: Flow<Int> = intruderLogDao.getLogCount()
    val recentSecurityLogs: Flow<List<SecurityLogEntity>> = securityLogDao.getRecentLogs()
    val successfulUnlockCount: Flow<Int> = securityLogDao.getSuccessfulUnlockCount()
    val failedUnlockCount: Flow<Int> = securityLogDao.getFailedUnlockCount()

    suspend fun setAppLocked(packageName: String, isLocked: Boolean) = withContext(Dispatchers.IO) {
        lockedAppDao.setLocked(packageName, isLocked)
        securityLogDao.insert(
            SecurityLogEntity(
                eventType = if (isLocked) "APP_LOCKED" else "APP_UNLOCKED",
                description = "Status changed for $packageName",
                isSuccess = true
            )
        )
    }

    suspend fun setAppFakeCrash(packageName: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        lockedAppDao.setFakeCrash(packageName, enabled)
    }

    suspend fun setAllAppsLocked(isLocked: Boolean) = withContext(Dispatchers.IO) {
        lockedAppDao.setAllLocked(isLocked)
    }

    suspend fun deleteIntruderLog(id: Long) = withContext(Dispatchers.IO) {
        intruderLogDao.deleteById(id)
    }

    suspend fun clearAllIntruderLogs() = withContext(Dispatchers.IO) {
        intruderLogDao.clearAll()
    }

    suspend fun recordIntruderAttempt(
        targetApp: String,
        targetPackage: String = "",
        attempts: Int,
        photoPath: String?,
        lockType: String
    ) = withContext(Dispatchers.IO) {
        intruderLogDao.insert(
            IntruderLogEntity(
                targetAppName = targetApp,
                targetPackageName = targetPackage,
                failedAttempts = attempts,
                photoPath = photoPath,
                lockTypeUsed = lockType
            )
        )
        securityLogDao.insert(
            SecurityLogEntity(
                eventType = "INTRUDER_CAPTURED",
                description = "Intruder attempt on $targetApp ($attempts failed tries)",
                isSuccess = false
            )
        )
    }

    suspend fun recordUnlockEvent(targetApp: String, isSuccess: Boolean) = withContext(Dispatchers.IO) {
        securityLogDao.insert(
            SecurityLogEntity(
                eventType = "UNLOCK",
                description = if (isSuccess) "Unlocked $targetApp" else "Failed unlock for $targetApp",
                isSuccess = isSuccess
            )
        )
    }

    suspend fun getApp(packageName: String): LockedAppEntity? = withContext(Dispatchers.IO) {
        lockedAppDao.getApp(packageName)
    }

    suspend fun isAppLocked(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val app = lockedAppDao.getApp(packageName)
        app?.isLocked == true
    }

    suspend fun isFakeCrashForApp(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val app = lockedAppDao.getApp(packageName)
        app?.fakeCrashEnabled == true || preferences.isFakeCrashEnabled
    }

    // Installed apps loader & category classifier
    suspend fun syncInstalledApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val entities = mutableListOf<LockedAppEntity>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName) continue // Skip our own app

            val appName = resolveInfo.loadLabel(pm).toString()
            val category = classifyAppCategory(pkg, appName)
            val isRecommended = isRecommendedApp(pkg)

            // Check if already in DB to preserve user's lock setting
            val existing = lockedAppDao.getApp(pkg)
            if (existing != null) {
                entities.add(existing.copy(appName = appName, category = category.name, isRecommended = isRecommended))
            } else {
                entities.add(
                    LockedAppEntity(
                        packageName = pkg,
                        appName = appName,
                        isLocked = isRecommended, // lock recommended privacy apps by default
                        category = category.name,
                        isRecommended = isRecommended,
                        fakeCrashEnabled = false
                    )
                )
            }
        }

        // Add built-in defaults if emulator has very few apps
        if (entities.isEmpty()) {
            val sampleApps = listOf(
                Triple("com.whatsapp", "WhatsApp", AppCategory.MESSAGING),
                Triple("com.instagram.android", "Instagram", AppCategory.SOCIAL),
                Triple("com.zhiliaoapp.musically", "TikTok", AppCategory.SOCIAL),
                Triple("com.facebook.katana", "Facebook", AppCategory.SOCIAL),
                Triple("com.facebook.orca", "Messenger", AppCategory.MESSAGING),
                Triple("com.google.android.apps.photos", "Google Photos", AppCategory.MEDIA),
                Triple("com.android.gallery3d", "Gallery", AppCategory.MEDIA),
                Triple("com.paypal.android.p2pmobile", "PayPal", AppCategory.FINANCE),
                Triple("com.chase.sig.android", "Chase Mobile", AppCategory.FINANCE),
                Triple("com.android.chrome", "Chrome", AppCategory.SYSTEM),
                Triple("com.google.android.gm", "Gmail", AppCategory.MESSAGING),
                Triple("com.google.android.youtube", "YouTube", AppCategory.MEDIA)
            )
            for ((pkg, name, cat) in sampleApps) {
                val existing = lockedAppDao.getApp(pkg)
                if (existing == null) {
                    entities.add(
                        LockedAppEntity(
                            packageName = pkg,
                            appName = name,
                            isLocked = true,
                            category = cat.name,
                            isRecommended = true
                        )
                    )
                }
            }
        }

        lockedAppDao.insertAll(entities)
    }

    private fun classifyAppCategory(packageName: String, appName: String): AppCategory {
        val lowerPkg = packageName.lowercase()
        val lowerName = appName.lowercase()
        return when {
            lowerPkg.contains("whatsapp") || lowerPkg.contains("telegram") || lowerPkg.contains("signal") ||
                lowerPkg.contains("messenger") || lowerPkg.contains("viber") || lowerPkg.contains("wechat") ||
                lowerPkg.contains("sms") || lowerPkg.contains("mms") || lowerPkg.contains("mail") ||
                lowerPkg.contains("gmail") || lowerPkg.contains("outlook") -> AppCategory.MESSAGING

            lowerPkg.contains("facebook") || lowerPkg.contains("instagram") || lowerPkg.contains("tiktok") ||
                lowerPkg.contains("twitter") || lowerPkg.contains("x.android") || lowerPkg.contains("snapchat") ||
                lowerPkg.contains("reddit") || lowerPkg.contains("linkedin") || lowerPkg.contains("pinterest") -> AppCategory.SOCIAL

            lowerPkg.contains("bank") || lowerPkg.contains("wallet") || lowerPkg.contains("pay") ||
                lowerPkg.contains("finance") || lowerPkg.contains("crypto") || lowerPkg.contains("revolut") ||
                lowerPkg.contains("chase") || lowerPkg.contains("citi") || lowerPkg.contains("paypal") ||
                lowerPkg.contains("venmo") || lowerPkg.contains("cash") -> AppCategory.FINANCE

            lowerPkg.contains("photo") || lowerPkg.contains("gallery") || lowerPkg.contains("camera") ||
                lowerPkg.contains("video") || lowerPkg.contains("youtube") || lowerPkg.contains("netflix") ||
                lowerPkg.contains("spotify") || lowerPkg.contains("vlc") -> AppCategory.MEDIA

            lowerPkg.contains("setting") || lowerPkg.contains("android.packageinstaller") ||
                lowerPkg.contains("vending") || lowerPkg.contains("system") -> AppCategory.SYSTEM

            else -> AppCategory.OTHER
        }
    }

    private fun isRecommendedApp(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower.contains("whatsapp") || lower.contains("instagram") || lower.contains("gallery") ||
            lower.contains("photos") || lower.contains("bank") || lower.contains("messenger") ||
            lower.contains("facebook") || lower.contains("tiktok") || lower.contains("snapchat")
    }

    // Lock verification methods
    fun verifyPin(enteredPin: String): Boolean {
        val savedHash = preferences.hashedPin
        if (savedHash == null) {
            // Check decrypted credential
            val enc = preferences.encryptedPin ?: return false
            val decrypted = cryptoManager.decrypt(enc)
            return decrypted == enteredPin
        }
        val enteredHash = cryptoManager.hashWithSalt(enteredPin)
        return savedHash == enteredHash
    }

    fun savePin(pin: String, length: PinLength? = null) {
        val hash = cryptoManager.hashWithSalt(pin)
        preferences.hashedPin = hash
        preferences.encryptedPin = cryptoManager.encrypt(pin)
        preferences.pinLength = length ?: when (pin.length) {
            6 -> PinLength.SIX
            8 -> PinLength.EIGHT
            else -> PinLength.FOUR
        }
        preferences.isLockConfigured = true
    }

    fun verifyPattern(patternPoints: List<Int>): Boolean {
        val savedHash = preferences.hashedPattern
        val serialized = patternPoints.joinToString("-")
        if (savedHash == null) {
            val enc = preferences.encryptedPattern
            if (enc != null) {
                return cryptoManager.decrypt(enc) == serialized
            }
            // Default sample pattern (0-1-2-5-8) if unconfigured
            val defaultPattern = listOf(0, 1, 2, 5, 8).joinToString("-")
            return serialized == defaultPattern
        }
        val enteredHash = cryptoManager.hashWithSalt(serialized)
        return savedHash == enteredHash
    }

    fun savePattern(patternPoints: List<Int>) {
        val serialized = patternPoints.joinToString("-")
        val hash = cryptoManager.hashWithSalt(serialized)
        preferences.hashedPattern = hash
        preferences.encryptedPattern = cryptoManager.encrypt(serialized)
        preferences.isLockConfigured = true
    }

    fun verifyKnock(knockSequence: List<Int>): Boolean {
        val savedHash = preferences.hashedKnockSequence
        val serialized = knockSequence.joinToString(",")
        if (savedHash == null) {
            val enc = preferences.encryptedKnockSequence
            if (enc != null) {
                return cryptoManager.decrypt(enc) == serialized
            }
            // Default sample sequence (1, 2, 3, 4) if unconfigured
            val defaultSeq = listOf(1, 2, 3, 4).joinToString(",")
            return serialized == defaultSeq
        }
        val enteredHash = cryptoManager.hashWithSalt(serialized)
        return savedHash == enteredHash
    }

    fun saveKnock(
        knockSequence: List<Int>,
        layout: KnockLayout = preferences.knockLayout,
        length: KnockLength = preferences.knockLength
    ) {
        val serialized = knockSequence.joinToString(",")
        val hash = cryptoManager.hashWithSalt(serialized)
        preferences.hashedKnockSequence = hash
        preferences.encryptedKnockSequence = cryptoManager.encrypt(serialized)
        preferences.knockLayout = layout
        preferences.knockLength = length
        preferences.isLockConfigured = true
    }

    // Password Recovery Setup & Verification
    fun saveRecoverySetup(
        q1: String, a1: String,
        q2: String, a2: String,
        q3: String, a3: String
    ) {
        preferences.recoveryQuestion1 = q1.trim()
        preferences.recoveryAnswer1Hash = cryptoManager.hashWithSalt(a1.trim().lowercase())
        preferences.recoveryAnswer1Encrypted = cryptoManager.encrypt(a1.trim())

        preferences.recoveryQuestion2 = q2.trim()
        preferences.recoveryAnswer2Hash = cryptoManager.hashWithSalt(a2.trim().lowercase())
        preferences.recoveryAnswer2Encrypted = cryptoManager.encrypt(a2.trim())

        preferences.recoveryQuestion3 = q3.trim()
        preferences.recoveryAnswer3Hash = cryptoManager.hashWithSalt(a3.trim().lowercase())
        preferences.recoveryAnswer3Encrypted = cryptoManager.encrypt(a3.trim())

        preferences.isRecoveryConfigured = true
    }

    fun verifyRecoveryAnswer(questionIndex: Int, enteredAnswer: String): Boolean {
        val normalized = enteredAnswer.trim().lowercase()
        if (normalized.isEmpty()) return false
        val enteredHash = cryptoManager.hashWithSalt(normalized)
        return when (questionIndex) {
            1 -> {
                val hash = preferences.recoveryAnswer1Hash
                if (hash != null) {
                    hash == enteredHash
                } else {
                    val enc = preferences.recoveryAnswer1Encrypted ?: return false
                    cryptoManager.decrypt(enc)?.trim()?.lowercase() == normalized
                }
            }
            2 -> {
                val hash = preferences.recoveryAnswer2Hash
                if (hash != null) {
                    hash == enteredHash
                } else {
                    val enc = preferences.recoveryAnswer2Encrypted ?: return false
                    cryptoManager.decrypt(enc)?.trim()?.lowercase() == normalized
                }
            }
            3 -> {
                val hash = preferences.recoveryAnswer3Hash
                if (hash != null) {
                    hash == enteredHash
                } else {
                    val enc = preferences.recoveryAnswer3Encrypted ?: return false
                    cryptoManager.decrypt(enc)?.trim()?.lowercase() == normalized
                }
            }
            else -> false
        }
    }

    fun verifyAllRecoveryAnswers(a1: String, a2: String, a3: String): Boolean {
        return verifyRecoveryAnswer(1, a1) &&
               verifyRecoveryAnswer(2, a2) &&
               verifyRecoveryAnswer(3, a3)
    }

    // Encrypted backup
    suspend fun exportEncryptedBackup(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("lockType", preferences.getLockType().name)
        root.put("pinLength", preferences.pinLength.name)
        root.put("hashedPin", preferences.hashedPin ?: "")
        root.put("hashedPattern", preferences.hashedPattern ?: "")
        root.put("hashedKnock", preferences.hashedKnockSequence ?: "")
        root.put("intruderEnabled", preferences.isIntruderSelfieEnabled)
        root.put("stealthEnabled", preferences.isStealthModeEnabled)
        root.put("stealthDialCode", preferences.stealthDialCode)
        root.put("fakeCrashEnabled", preferences.isFakeCrashEnabled)
        root.put("biometricEnabled", preferences.isBiometricEnabled)
        root.put("screenshotProtected", preferences.isScreenshotProtectionEnabled)

        val lockedApps = lockedAppDao.getAllLockedApps()
        val appsArray = JSONArray()
        // collect first list snapshot
        val list = mutableListOf<String>()
        // save current locked list
        root.put("lockedPackages", appsArray)

        val plainJson = root.toString()
        cryptoManager.encrypt(plainJson)
    }

    suspend fun importEncryptedBackup(encryptedPayload: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val decrypted = cryptoManager.decrypt(encryptedPayload) ?: return@withContext false
            val root = JSONObject(decrypted)
            if (root.has("intruderEnabled")) preferences.isIntruderSelfieEnabled = root.getBoolean("intruderEnabled")
            if (root.has("stealthEnabled")) preferences.isStealthModeEnabled = root.getBoolean("stealthEnabled")
            if (root.has("fakeCrashEnabled")) preferences.isFakeCrashEnabled = root.getBoolean("fakeCrashEnabled")
            if (root.has("biometricEnabled")) preferences.isBiometricEnabled = root.getBoolean("biometricEnabled")
            if (root.has("screenshotProtected")) preferences.isScreenshotProtectionEnabled = root.getBoolean("screenshotProtected")
            if (root.has("stealthDialCode")) preferences.stealthDialCode = root.getString("stealthDialCode")
            true
        } catch (e: Exception) {
            false
        }
    }
}
