package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.service.AppMonitorService
import com.example.ui.navigation.Screen
import com.example.ui.screens.AppListScreen
import com.example.ui.screens.ChangeLockScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.IntruderReportScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.theme.NLockTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = NLockApplication.instance.preferences

        if (preferences.isLockConfigured) {
            try {
                AppMonitorService.startService(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            val themeMode by preferences.themeFlow.collectAsState()

            NLockTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                val startDestination = if (preferences.isOnboardingCompleted) {
                    Screen.Dashboard.route
                } else {
                    Screen.Onboarding.route
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Onboarding.route) {
                        OnboardingScreen(
                            onFinished = {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            onNavigateToApps = { navController.navigate(Screen.AppList.route) },
                            onNavigateToIntruders = { navController.navigate(Screen.IntruderReports.route) },
                            onNavigateToStats = { navController.navigate(Screen.Statistics.route) },
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                            onNavigateToChangeLock = { navController.navigate(Screen.ChangeLock.route) },
                            onNavigateToScreenTime = { navController.navigate(Screen.ScreenTime.route) },
                            onTestLock = { navController.navigate(Screen.LockTest.route) }
                        )
                    }

                    composable(Screen.ScreenTime.route) {
                        com.example.ui.screens.ScreenTimeManagerScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.AppList.route) {
                        AppListScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.IntruderReports.route) {
                        IntruderReportScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Statistics.route) {
                        StatisticsScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onChangeLockMethod = { navController.navigate(Screen.ChangeLock.route) }
                        )
                    }

                    composable(Screen.ChangeLock.route) {
                        ChangeLockScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.LockTest.route) {
                        LockScreen(
                            targetAppName = "N Lock Security Test",
                            targetPackageName = packageName,
                            onUnlocked = { navController.popBackStack() },
                            onCancelled = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

// Keep Greeting for screenshot test compatibility
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NLockTheme { Greeting("Android") }
}
