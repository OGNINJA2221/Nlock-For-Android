package com.example.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.example.LockActivity
import com.example.NLockApplication
import com.example.security.LockSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppLockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkgName = event.packageName?.toString() ?: return
            if (pkgName.isBlank() || pkgName == packageName) return

            // Check if App Lock is master enabled
            val preferences = NLockApplication.instance.preferences
            if (!preferences.isAppLockEnabled) return

            // Delegate to LockSessionManager to handle transitions, transient overlays, and timeouts
            LockSessionManager.onPackageForegroundChanged(pkgName, this)

            // Ignore transient system packages (SystemUI, Keyboard, System Dialogs) and Home Launcher
            if (LockSessionManager.isTransientSystemPackage(pkgName, this) ||
                LockSessionManager.isLauncherPackage(pkgName, this)
            ) {
                return
            }

            serviceScope.launch {
                val repository = NLockApplication.instance.repository
                if (repository.isAppLocked(pkgName) && !LockSessionManager.isSessionUnlocked(pkgName)) {
                    val appName = repository.getApp(pkgName)?.appName
                    LockActivity.launchLock(this@AppLockAccessibilityService, pkgName, appName)
                }
            }
        }
    }

    override fun onInterrupt() {}
}
