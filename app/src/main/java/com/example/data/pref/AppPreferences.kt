package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppThemeMode
import com.example.data.model.KnockLayout
import com.example.data.model.KnockLength
import com.example.data.model.LockType
import com.example.data.model.PinLength
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nlock_secure_prefs", Context.MODE_PRIVATE)

    private val _themeFlow = MutableStateFlow(getAppTheme())
    val themeFlow: StateFlow<AppThemeMode> = _themeFlow.asStateFlow()

    private val _lockTypeFlow = MutableStateFlow(getLockType())
    val lockTypeFlow: StateFlow<LockType> = _lockTypeFlow.asStateFlow()

    private val _appLockEnabledFlow = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK_ENABLED, true))
    val appLockEnabledFlow: StateFlow<Boolean> = _appLockEnabledFlow.asStateFlow()

    var isAppLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, value).apply()
            _appLockEnabledFlow.value = value
        }

    var isHapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, value).apply()

    var isAnimationEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANIMATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ANIMATION_ENABLED, value).apply()

    var weeklyScreenTimeGoalMinutes: Int
        get() = prefs.getInt(KEY_WEEKLY_SCREEN_TIME_MINUTES, 180) // default 3 hours = 180 mins
        set(value) = prefs.edit().putInt(KEY_WEEKLY_SCREEN_TIME_MINUTES, value).apply()

    var isScreenTimeGoalEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCREEN_TIME_GOAL_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SCREEN_TIME_GOAL_ENABLED, value).apply()

    var lastNotifiedGoalMilestone: Int
        get() = prefs.getInt(KEY_LAST_NOTIFIED_MILESTONE, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_NOTIFIED_MILESTONE, value).apply()

    var lastNotifiedWeekNumber: Int
        get() = prefs.getInt(KEY_LAST_NOTIFIED_WEEK, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_NOTIFIED_WEEK, value).apply()

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var isLockConfigured: Boolean
        get() = prefs.getBoolean(KEY_LOCK_CONFIGURED, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_CONFIGURED, value).apply()

    var pinLength: PinLength
        get() = PinLength.entries.find { it.name == prefs.getString(KEY_PIN_LENGTH, PinLength.FOUR.name) }
            ?: PinLength.FOUR
        set(value) = prefs.edit().putString(KEY_PIN_LENGTH, value.name).apply()

    var hashedPin: String?
        get() = prefs.getString(KEY_HASHED_PIN, null)
        set(value) = prefs.edit().putString(KEY_HASHED_PIN, value).apply()

    var encryptedPin: String?
        get() = prefs.getString(KEY_ENCRYPTED_PIN, null)
        set(value) = prefs.edit().putString(KEY_ENCRYPTED_PIN, value).apply()

    var hashedPattern: String?
        get() = prefs.getString(KEY_HASHED_PATTERN, null)
        set(value) = prefs.edit().putString(KEY_HASHED_PATTERN, value).apply()

    var encryptedPattern: String?
        get() = prefs.getString(KEY_ENCRYPTED_PATTERN, null)
        set(value) = prefs.edit().putString(KEY_ENCRYPTED_PATTERN, value).apply()

    var hashedKnockSequence: String?
        get() = prefs.getString(KEY_HASHED_KNOCK, null)
        set(value) = prefs.edit().putString(KEY_HASHED_KNOCK, value).apply()

    var encryptedKnockSequence: String?
        get() = prefs.getString(KEY_ENCRYPTED_KNOCK, null)
        set(value) = prefs.edit().putString(KEY_ENCRYPTED_KNOCK, value).apply()

    var knockLayout: KnockLayout
        get() = KnockLayout.entries.find { it.name == prefs.getString(KEY_KNOCK_LAYOUT, KnockLayout.GRID_2X2_CENTER.name) }
            ?: KnockLayout.GRID_2X2_CENTER
        set(value) = prefs.edit().putString(KEY_KNOCK_LAYOUT, value.name).apply()

    var knockLength: KnockLength
        get() = KnockLength.entries.find { it.name == prefs.getString(KEY_KNOCK_LENGTH, KnockLength.FOUR.name) }
            ?: KnockLength.FOUR
        set(value) = prefs.edit().putString(KEY_KNOCK_LENGTH, value.name).apply()

    var isIntruderSelfieEnabled: Boolean
        get() = prefs.getBoolean(KEY_INTRUDER_SELFIE, true)
        set(value) = prefs.edit().putBoolean(KEY_INTRUDER_SELFIE, value).apply()

    var intruderThreshold: Int
        get() = prefs.getInt(KEY_INTRUDER_THRESHOLD, 3)
        set(value) = prefs.edit().putInt(KEY_INTRUDER_THRESHOLD, value).apply()

    var isStealthModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_STEALTH_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_STEALTH_MODE, value).apply()

    var stealthDialCode: String
        get() = prefs.getString(KEY_STEALTH_DIAL_CODE, "*#*#6625#*#*") ?: "*#*#6625#*#*"
        set(value) = prefs.edit().putString(KEY_STEALTH_DIAL_CODE, value).apply()

    var isFakeCrashEnabled: Boolean
        get() = prefs.getBoolean(KEY_FAKE_CRASH, false)
        set(value) = prefs.edit().putBoolean(KEY_FAKE_CRASH, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    var isScreenshotProtectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCREENSHOT_PROTECTION, false)
        set(value) = prefs.edit().putBoolean(KEY_SCREENSHOT_PROTECTION, value).apply()

    var isImmediateLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_IMMEDIATE_LOCK, true)
        set(value) = prefs.edit().putBoolean(KEY_IMMEDIATE_LOCK, value).apply()

    var isImmediateLock: Boolean
        get() = isImmediateLockEnabled
        set(value) { isImmediateLockEnabled = value }

    var lockTimeoutSeconds: Int
        get() = prefs.getInt(KEY_LOCK_TIMEOUT_SECONDS, 30)
        set(value) = prefs.edit().putInt(KEY_LOCK_TIMEOUT_SECONDS, value).apply()

    var failedAttemptsCount: Int
        get() = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        set(value) = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, value).apply()

    var bruteForceCooldownUntil: Long
        get() = prefs.getLong(KEY_BRUTE_FORCE_COOLDOWN_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_BRUTE_FORCE_COOLDOWN_UNTIL, value).apply()

    fun recordFailedAttempt(): Long {
        val newCount = failedAttemptsCount + 1
        failedAttemptsCount = newCount

        val cooldownSeconds = when {
            newCount >= 20 -> 15 * 60 // 15 minutes cooldown
            newCount >= 15 -> 5 * 60  // 5 minutes cooldown
            newCount >= 10 -> 60      // 60 seconds cooldown
            newCount >= 5 -> 30       // 30 seconds cooldown
            else -> 0
        }

        if (cooldownSeconds > 0) {
            val cooldownUntil = System.currentTimeMillis() + (cooldownSeconds * 1000L)
            bruteForceCooldownUntil = cooldownUntil
            return cooldownSeconds.toLong()
        }
        return 0L
    }

    fun resetFailedAttempts() {
        failedAttemptsCount = 0
        bruteForceCooldownUntil = 0L
    }

    fun getRemainingCooldownSeconds(): Long {
        val until = bruteForceCooldownUntil
        val now = System.currentTimeMillis()
        return if (until > now) (until - now) / 1000 else 0L
    }

    var isRecoveryConfigured: Boolean
        get() = prefs.getBoolean(KEY_RECOVERY_CONFIGURED, false)
        set(value) = prefs.edit().putBoolean(KEY_RECOVERY_CONFIGURED, value).apply()

    var recoveryQuestion1: String
        get() = prefs.getString(KEY_RECOVERY_Q1, "What was your first pet's name?") ?: "What was your first pet's name?"
        set(value) = prefs.edit().putString(KEY_RECOVERY_Q1, value).apply()

    var recoveryAnswer1Hash: String?
        get() = prefs.getString(KEY_RECOVERY_A1_HASH, null)
        set(value) = prefs.edit().putString(KEY_RECOVERY_A1_HASH, value).apply()

    var recoveryAnswer1Encrypted: String?
        get() = prefs.getString(KEY_RECOVERY_A1_ENC, null)
        set(value) = prefs.edit().putString(KEY_RECOVERY_A1_ENC, value).apply()

    var recoveryQuestion2: String
        get() = prefs.getString(KEY_RECOVERY_Q2, "What city were you born in?") ?: "What city were you born in?"
        set(value) = prefs.edit().putString(KEY_RECOVERY_Q2, value).apply()

    var recoveryAnswer2Hash: String?
        get() = prefs.getString(KEY_RECOVERY_A2_HASH, null)
        set(value) = prefs.edit().putString(KEY_RECOVERY_A2_HASH, value).apply()

    var recoveryAnswer2Encrypted: String?
        get() = prefs.getString(KEY_RECOVERY_A2_ENC, null)
        set(value) = prefs.edit().putString(KEY_RECOVERY_A2_ENC, value).apply()

    var recoveryQuestion3: String
        get() = prefs.getString(KEY_RECOVERY_Q3, "What is your favorite food?") ?: "What is your favorite food?"
        set(value) = prefs.edit().putString(KEY_RECOVERY_Q3, value).apply()

    var recoveryAnswer3Hash: String?
        get() = prefs.getString(KEY_RECOVERY_A3_HASH, null)
        set(value) = prefs.edit().putString(KEY_RECOVERY_A3_HASH, value).apply()

    var recoveryAnswer3Encrypted: String?
        get() = prefs.getString(KEY_RECOVERY_A3_ENC, null)
        set(value) = prefs.edit().putString(KEY_RECOVERY_A3_ENC, value).apply()

    var securityQuestion: String
        get() = prefs.getString(KEY_SECURITY_QUESTION, "What is your favorite security passkey?") ?: "What is your favorite security passkey?"
        set(value) = prefs.edit().putString(KEY_SECURITY_QUESTION, value).apply()

    var securityAnswerHash: String?
        get() = prefs.getString(KEY_SECURITY_ANSWER_HASH, null)
        set(value) = prefs.edit().putString(KEY_SECURITY_ANSWER_HASH, value).apply()

    fun getLockType(): LockType {
        val name = prefs.getString(KEY_LOCK_TYPE, LockType.PIN.name)
        return LockType.entries.find { it.name == name } ?: LockType.PIN
    }

    fun setLockType(type: LockType) {
        prefs.edit().putString(KEY_LOCK_TYPE, type.name).apply()
        _lockTypeFlow.value = type
    }

    fun getAppTheme(): AppThemeMode {
        val name = prefs.getString(KEY_APP_THEME, AppThemeMode.DEFAULT_GLASS_BLUE.name)
        return AppThemeMode.entries.find { it.name == name } ?: AppThemeMode.DEFAULT_GLASS_BLUE
    }

    fun setAppTheme(theme: AppThemeMode) {
        prefs.edit().putString(KEY_APP_THEME, theme.name).apply()
        _themeFlow.value = theme
    }

    /**
     * Exports non-sensitive preferences as a formatted JSON string for backup.
     */
    fun exportSettingsJson(): String {
        val json = org.json.JSONObject().apply {
            put("app_lock_enabled", isAppLockEnabled)
            put("immediate_lock", isImmediateLockEnabled)
            put("lock_timeout_seconds", lockTimeoutSeconds)
            put("app_theme", getAppTheme().name)
            put("biometric_enabled", isBiometricEnabled)
            put("intruder_selfie_enabled", isIntruderSelfieEnabled)
            put("intruder_threshold", intruderThreshold)
            put("fake_crash_enabled", isFakeCrashEnabled)
            put("stealth_mode_enabled", isStealthModeEnabled)
            put("stealth_dial_code", stealthDialCode)
            put("screenshot_protection", isScreenshotProtectionEnabled)
            put("haptic_feedback", isHapticFeedbackEnabled)
            put("animation_enabled", isAnimationEnabled)
            put("screen_time_goal_enabled", isScreenTimeGoalEnabled)
            put("weekly_screen_time_minutes", weeklyScreenTimeGoalMinutes)
            put("lock_type", getLockType().name)
        }
        return json.toString(2)
    }

    /**
     * Imports configuration from JSON string.
     */
    fun importSettingsJson(jsonStr: String): Boolean {
        return try {
            val json = org.json.JSONObject(jsonStr)
            if (json.has("app_lock_enabled")) isAppLockEnabled = json.getBoolean("app_lock_enabled")
            if (json.has("immediate_lock")) isImmediateLockEnabled = json.getBoolean("immediate_lock")
            if (json.has("lock_timeout_seconds")) lockTimeoutSeconds = json.getInt("lock_timeout_seconds")
            if (json.has("app_theme")) {
                val themeName = json.getString("app_theme")
                AppThemeMode.entries.find { it.name == themeName }?.let { setAppTheme(it) }
            }
            if (json.has("biometric_enabled")) isBiometricEnabled = json.getBoolean("biometric_enabled")
            if (json.has("intruder_selfie_enabled")) isIntruderSelfieEnabled = json.getBoolean("intruder_selfie_enabled")
            if (json.has("intruder_threshold")) intruderThreshold = json.getInt("intruder_threshold")
            if (json.has("fake_crash_enabled")) isFakeCrashEnabled = json.getBoolean("fake_crash_enabled")
            if (json.has("stealth_mode_enabled")) isStealthModeEnabled = json.getBoolean("stealth_mode_enabled")
            if (json.has("stealth_dial_code")) stealthDialCode = json.getString("stealth_dial_code")
            if (json.has("screenshot_protection")) isScreenshotProtectionEnabled = json.getBoolean("screenshot_protection")
            if (json.has("haptic_feedback")) isHapticFeedbackEnabled = json.getBoolean("haptic_feedback")
            if (json.has("animation_enabled")) isAnimationEnabled = json.getBoolean("animation_enabled")
            if (json.has("screen_time_goal_enabled")) isScreenTimeGoalEnabled = json.getBoolean("screen_time_goal_enabled")
            if (json.has("weekly_screen_time_minutes")) weeklyScreenTimeGoalMinutes = json.getInt("weekly_screen_time_minutes")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        private const val KEY_ANIMATION_ENABLED = "animation_enabled"
        private const val KEY_WEEKLY_SCREEN_TIME_MINUTES = "weekly_screen_time_minutes"
        private const val KEY_SCREEN_TIME_GOAL_ENABLED = "screen_time_goal_enabled"
        private const val KEY_LAST_NOTIFIED_MILESTONE = "last_notified_milestone"
        private const val KEY_LAST_NOTIFIED_WEEK = "last_notified_week"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_LOCK_CONFIGURED = "lock_configured"
        private const val KEY_LOCK_TYPE = "lock_type"
        private const val KEY_PIN_LENGTH = "pin_length"
        private const val KEY_HASHED_PIN = "hashed_pin"
        private const val KEY_ENCRYPTED_PIN = "encrypted_pin"
        private const val KEY_HASHED_PATTERN = "hashed_pattern"
        private const val KEY_ENCRYPTED_PATTERN = "encrypted_pattern"
        private const val KEY_HASHED_KNOCK = "hashed_knock"
        private const val KEY_ENCRYPTED_KNOCK = "encrypted_knock"
        private const val KEY_KNOCK_LAYOUT = "knock_layout"
        private const val KEY_KNOCK_LENGTH = "knock_length"
        private const val KEY_INTRUDER_SELFIE = "intruder_selfie"
        private const val KEY_INTRUDER_THRESHOLD = "intruder_threshold"
        private const val KEY_STEALTH_MODE = "stealth_mode"
        private const val KEY_STEALTH_DIAL_CODE = "stealth_dial_code"
        private const val KEY_FAKE_CRASH = "fake_crash"
        private const val KEY_BIOMETRIC = "biometric"
        private const val KEY_SCREENSHOT_PROTECTION = "screenshot_protection"
        private const val KEY_IMMEDIATE_LOCK = "lock_instantly_when_app_closes"
        private const val KEY_LOCK_TIMEOUT_SECONDS = "lock_timeout_seconds"
        private const val KEY_FAILED_ATTEMPTS = "brute_force_failed_attempts"
        private const val KEY_BRUTE_FORCE_COOLDOWN_UNTIL = "brute_force_cooldown_until"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_SECURITY_QUESTION = "security_question"
        private const val KEY_SECURITY_ANSWER_HASH = "security_answer_hash"
        private const val KEY_RECOVERY_CONFIGURED = "recovery_configured"
        private const val KEY_RECOVERY_Q1 = "recovery_q1"
        private const val KEY_RECOVERY_A1_HASH = "recovery_a1_hash"
        private const val KEY_RECOVERY_A1_ENC = "recovery_a1_enc"
        private const val KEY_RECOVERY_Q2 = "recovery_q2"
        private const val KEY_RECOVERY_A2_HASH = "recovery_a2_hash"
        private const val KEY_RECOVERY_A2_ENC = "recovery_a2_enc"
        private const val KEY_RECOVERY_Q3 = "recovery_q3"
        private const val KEY_RECOVERY_A3_HASH = "recovery_a3_hash"
        private const val KEY_RECOVERY_A3_ENC = "recovery_a3_enc"
    }
}
