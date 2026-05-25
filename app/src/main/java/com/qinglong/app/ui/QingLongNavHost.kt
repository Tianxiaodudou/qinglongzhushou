package com.qinglong.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.qinglong.app.ui.navigation.Screen
import com.qinglong.app.ui.navigation.mainDestinations
import com.qinglong.app.ui.screens.login.LoginScreen
import com.qinglong.app.ui.screens.login.LoginViewModel
import com.qinglong.app.ui.screens.splash.SplashScreen
import com.qinglong.app.ui.screens.task.TaskScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QingLongNavHost() {
    val navController = rememberNavController()
    val authViewModel: LoginViewModel = hiltViewModel()
    val currentRoute = navController.currentBackStackEntryFlow.collectAsState(initial = null).value?.destination?.route ?: ""

    // 导航菜单状态
    var showNavMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.fillMaxSize()
        ) {
            // 启动动画页面
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Task.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Task.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Task.route) {
                TaskScreen(
                    onMenuClick = { showNavMenu = true }
                )
            }

            // Placeholder routes
            composable(Screen.Subscription.route) {
                com.qinglong.app.ui.screens.PlaceholderScreen("订阅管理")
            }

            composable(Screen.Log.route) {
                com.qinglong.app.ui.screens.PlaceholderScreen("日志管理")
            }

            composable(Screen.EnvVar.route) {
                com.qinglong.app.ui.screens.PlaceholderScreen("环境变量")
            }

            composable(Screen.System.route) {
                com.qinglong.app.ui.screens.PlaceholderScreen("系统状态")
            }

            composable(Screen.PanelSettings.route) {
                com.qinglong.app.ui.screens.PlaceholderScreen("面板设置")
            }

            composable(Screen.AppSettings.route) {
                com.qinglong.app.ui.screens.PlaceholderScreen("应用设置")
            }
        }

        // 导航菜单（覆盖在最上层）
        if (showNavMenu) {
            NavigationMenuDialog(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    showNavMenu = false
                    navController.navigate(route) {
                        popUpTo(Screen.Task.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onSwitchServer = {
                    showNavMenu = false
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onLogout = {
                    showNavMenu = false
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onDismiss = { showNavMenu = false }
            )
        }
    }
}

@Composable
fun NavigationMenuDialog(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onSwitchServer: () -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导航菜单") },
        text = {
            Column {
                mainDestinations.forEach { dest ->
                    val isSelected = currentRoute == dest.route
                    TextButton(
                        onClick = { onNavigate(dest.route) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(dest.title)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TextButton(
                    onClick = onSwitchServer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("切换服务器")
                }
                TextButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("退出登录")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
