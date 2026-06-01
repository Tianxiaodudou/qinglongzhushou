@file:Suppress("DEPRECATION")

package com.qinglong.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Task : Screen("task")
    object Subscription : Screen("subscription")
    object Log : Screen("log")
    object EnvVar : Screen("envvar")
    object Script : Screen("script")
    object Config : Screen("config")
    object Dependence : Screen("dependence")
    object PanelSettings : Screen("panel_settings")
    object Notification : Screen("notification")
    object SecuritySettings : Screen("security_settings")
    object AppManagement : Screen("app_management")
    object DependenceSettings : Screen("dependence_settings")
    object OtherSettings : Screen("other_settings")
    object SystemLog : Screen("system_log")
    object LoginLog : Screen("login_log")
    object AppSettings : Screen("app_settings")
}

data class NavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
)

val mainDestinations = listOf(
    NavDestination(Screen.Task.route, "定时任务", Icons.Default.Schedule),
    NavDestination(Screen.Subscription.route, "订阅管理", Icons.Default.Subscriptions),
    NavDestination(Screen.Log.route, "日志管理", Icons.Default.Article),
    NavDestination(Screen.EnvVar.route, "环境变量", Icons.Default.Code),
    NavDestination(Screen.Notification.route, "通知设置", Icons.Default.Notifications),
    NavDestination(Screen.PanelSettings.route, "面板设置", Icons.Default.Settings),
    NavDestination(Screen.AppSettings.route, "应用设置", Icons.Default.PhoneAndroid)
)
