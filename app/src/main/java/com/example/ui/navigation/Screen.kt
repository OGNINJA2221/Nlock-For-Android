package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object AppList : Screen("app_list")
    data object IntruderReports : Screen("intruder_reports")
    data object Statistics : Screen("statistics")
    data object Settings : Screen("settings")
    data object LockTest : Screen("lock_test")
    data object ChangeLock : Screen("change_lock")
    data object ScreenTime : Screen("screen_time")
}
