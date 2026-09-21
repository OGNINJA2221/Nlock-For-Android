package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.security.LockSessionManager
import com.example.ui.screens.LockScreen
import com.example.ui.theme.NLockTheme

class LockActivity : FragmentActivity() {

    private var targetPackage: String = ""
    private var targetAppName: String = "Protected Application"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = NLockApplication.instance.preferences
        val securityManager = NLockApplication.instance.securityManager
        if (preferences.isScreenshotProtectionEnabled && !securityManager.isRunningOnEmulator()) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        if (savedInstanceState != null) {
            targetPackage = savedInstanceState.getString(KEY_SAVED_PACKAGE, "")
            targetAppName = savedInstanceState.getString(KEY_SAVED_APP_NAME, "Protected Application")
        } else {
            targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
            targetAppName = intent.getStringExtra(EXTRA_APP_NAME)
                ?: if (targetPackage.isNotEmpty()) {
                    targetPackage.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                } else {
                    "Protected Application"
                }
        }

        setContent {
            NLockTheme(themeMode = preferences.getAppTheme()) {
                LockScreen(
                    targetAppName = targetAppName,
                    targetPackageName = targetPackage,
                    onUnlocked = {
                        // Mark current session unlocked
                        markSessionUnlocked(targetPackage)
                        preferences.resetFailedAttempts()

                        // Resume the exact application without resetting task stack
                        if (targetPackage.isNotEmpty() && targetPackage != packageName) {
                            try {
                                val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)?.apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                if (launchIntent != null) {
                                    startActivity(launchIntent)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        // Close authentication screen immediately. Never open MainActivity or Dashboard.
                        finish()
                    },
                    onCancelled = {
                        // Cancel/back: minimize to launcher/home without unlocking
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finishAndRemoveTask()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newPkg = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        if (!newPkg.isNullOrEmpty()) {
            targetPackage = newPkg
            targetAppName = intent.getStringExtra(EXTRA_APP_NAME)
                ?: newPkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SAVED_PACKAGE, targetPackage)
        outState.putString(KEY_SAVED_APP_NAME, targetAppName)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent bypassing lock by pressing back - minimize to home and finish lock
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finishAndRemoveTask()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_target_package"
        const val EXTRA_APP_NAME = "extra_target_app_name"
        private const val KEY_SAVED_PACKAGE = "saved_target_package"
        private const val KEY_SAVED_APP_NAME = "saved_target_app_name"

        // Delegate session management to LockSessionManager
        fun markSessionUnlocked(packageName: String) {
            LockSessionManager.markSessionUnlocked(packageName)
        }

        fun isSessionUnlocked(packageName: String): Boolean {
            return LockSessionManager.isSessionUnlocked(packageName)
        }

        fun relockApp(packageName: String) {
            LockSessionManager.relockApp(packageName)
        }

        fun clearAllUnlockedSessions() {
            LockSessionManager.clearAllSessions()
        }

        fun launchLock(context: Context, packageName: String, appName: String? = null) {
            val intent = Intent(context, LockActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_APP_NAME, appName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            context.startActivity(intent)
        }
    }
}
