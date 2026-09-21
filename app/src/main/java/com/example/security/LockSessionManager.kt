package com.example.security

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.inputmethod.InputMethodManager
import com.example.NLockApplication
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages per-app unlocked sessions and anti-annoyance session tracking.
 *
 * Ensures that once an app is unlocked:
 * - Navigating between pages, fragments, sub-activities, or tabs inside the app NEVER prompts for PIN.
 * - Clicking notifications, pulling down the notification shade (SystemUI), or using keyboards NEVER prompts for PIN.
 * - When the user leaves the app:
 *   - If "Lock Instantly When App Closes" is ON: The app locks immediately.
 *   - If "Lock Instantly When App Closes" is OFF: The app remains unlocked for the duration of the Lock Timer (e.g., 30s, 60s, custom).
 * - Opening a different protected app properly requires authentication for that app.
 */
object LockSessionManager {

    data class AppSession(
        val packageName: String,
        val unlockTimestamp: Long,
        var lastForegroundTimestamp: Long
    )

    // Active unlocked sessions: packageName -> AppSession
    private val activeSessions = ConcurrentHashMap<String, AppSession>()

    // Current non-transient foreground package
    @Volatile
    private var currentForegroundPackage: String? = null

    // Cache launcher packages so home screen transitions are fast
    @Volatile
    private var cachedLauncherPackages: Set<String>? = null

    /**
     * Records an app as successfully unlocked with timestamp.
     */
    fun markSessionUnlocked(packageName: String) {
        if (packageName.isBlank()) return
        val now = System.currentTimeMillis()
        activeSessions[packageName] = AppSession(
            packageName = packageName,
            unlockTimestamp = now,
            lastForegroundTimestamp = now
        )
        currentForegroundPackage = packageName
    }

    /**
     * Checks if the given app is currently unlocked and session is valid.
     */
    fun isSessionUnlocked(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        val session = activeSessions[packageName] ?: return false
        val preferences = NLockApplication.instance.preferences

        val isImmediate = preferences.isImmediateLockEnabled
        val now = System.currentTimeMillis()

        if (isImmediate) {
            // If immediate lock is active, the app remains unlocked only while the user
            // has not left the package.
            return if (currentForegroundPackage == packageName) {
                session.lastForegroundTimestamp = now
                true
            } else {
                activeSessions.remove(packageName)
                false
            }
        } else {
            // Lock Timer timeout system
            if (currentForegroundPackage == packageName) {
                // User is still actively in the app
                session.lastForegroundTimestamp = now
                return true
            }

            val timeoutSeconds = preferences.lockTimeoutSeconds
            val elapsedSeconds = (now - session.lastForegroundTimestamp) / 1000L

            return if (elapsedSeconds <= timeoutSeconds) {
                // Session is still within grace period!
                true
            } else {
                // Timeout expired
                activeSessions.remove(packageName)
                false
            }
        }
    }

    /**
     * Handles foreground package changes, carefully distinguishing between
     * transient system overlays (SystemUI, Keyboards, Autocomplete, Permission dialogs)
     * and genuine application/launcher switches.
     */
    fun onPackageForegroundChanged(newPackageName: String, context: Context) {
        if (newPackageName.isBlank()) return

        // 1. If it's N Lock itself, ignore (lock screen or app configuration)
        if (newPackageName == context.packageName) return

        // 2. If it's a transient system package (SystemUI notification shade, keyboard, permission dialog),
        // DO NOT treat it as the user leaving the app! The user is still inside the target app session.
        if (isTransientSystemPackage(newPackageName, context)) {
            return
        }

        // 3. User is in the same app (sub-activity, fragment, page, video, settings within app)
        if (newPackageName == currentForegroundPackage) {
            activeSessions[newPackageName]?.lastForegroundTimestamp = System.currentTimeMillis()
            return
        }

        // 4. User switched away from previous app to another app or home screen
        val previousPackage = currentForegroundPackage
        if (previousPackage != null && previousPackage != newPackageName) {
            val prevSession = activeSessions[previousPackage]
            if (prevSession != null) {
                prevSession.lastForegroundTimestamp = System.currentTimeMillis()
                val preferences = NLockApplication.instance.preferences
                if (preferences.isImmediateLockEnabled) {
                    activeSessions.remove(previousPackage)
                }
            }
        }

        currentForegroundPackage = newPackageName

        // If user returned to a session that is still within timeout, update its active time
        val currentSession = activeSessions[newPackageName]
        if (currentSession != null) {
            currentSession.lastForegroundTimestamp = System.currentTimeMillis()
        }
    }

    /**
     * Returns true if the package represents transient system UI:
     * - Notification shade / Quick Settings (com.android.systemui)
     * - Soft keyboards / Input Method Engines
     * - Android system dialogs & permission controller
     * - Media & Photo pickers called from within an app
     */
    fun isTransientSystemPackage(pkgName: String, context: Context): Boolean {
        if (pkgName == context.packageName) return true
        if (pkgName == "com.android.systemui") return true
        if (pkgName == "android") return true
        if (pkgName == "com.google.android.gms") return true
        if (pkgName.contains("permissioncontroller")) return true
        if (pkgName == "com.android.documentsui") return true
        if (pkgName.contains("providers.media")) return true
        if (pkgName == "com.google.android.googlequicksearchbox") return true
        if (isInputMethodPackage(pkgName, context)) return true
        return false
    }

    /**
     * Checks if a package is a keyboard / input method.
     */
    private fun isInputMethodPackage(pkgName: String, context: Context): Boolean {
        val lower = pkgName.lowercase()
        if (lower.contains("inputmethod") || lower.contains(".ime") || lower.contains("keyboard") || lower.contains("latin")) {
            return true
        }
        return try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.inputMethodList?.any { it.packageName == pkgName } == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks whether a package is a home screen launcher.
     */
    fun isLauncherPackage(pkgName: String, context: Context): Boolean {
        val cached = cachedLauncherPackages
        if (cached != null && cached.contains(pkgName)) return true

        return try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            val launchers = resolveInfos.mapNotNull { it.activityInfo?.packageName }.toSet()
            cachedLauncherPackages = launchers
            launchers.contains(pkgName)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Manually relocks an application immediately (e.g. forced relock).
     */
    fun relockApp(packageName: String) {
        activeSessions.remove(packageName)
    }

    /**
     * Clears all unlocked sessions.
     */
    fun clearAllSessions() {
        activeSessions.clear()
        currentForegroundPackage = null
    }

    /**
     * Formats seconds into human-readable label.
     */
    fun formatTimeout(seconds: Int): String {
        return when {
            seconds < 60 -> "$seconds sec"
            seconds < 3600 -> {
                val mins = seconds / 60
                val remSecs = seconds % 60
                if (remSecs == 0) "$mins min" else "$mins min $remSecs sec"
            }
            else -> {
                val hours = seconds / 3600
                val remMins = (seconds % 3600) / 60
                if (remMins == 0) "$hours hour${if (hours > 1) "s" else ""}" else "$hours hr $remMins min"
            }
        }
    }
}
