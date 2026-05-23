package com.qinglong.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.qinglong.app.data.repository.AuthRepository
import com.qinglong.app.ui.components.QingLongDrawer
import com.qinglong.app.ui.navigation.Screen
import com.qinglong.app.ui.navigation.mainDestinations
import com.qinglong.app.ui.screens.login.LoginScreen
import com.qinglong.app.ui.screens.login.LoginViewModel
import com.qinglong.app.ui.screens.task.TaskScreen
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QingLongNavHost() {
    val navController = rememberNavController()
    val authViewModel: LoginViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val startDestination = if (isLoggedIn) Screen.Task.route else Screen.Login.route
    val currentRoute = navController.currentBackStackEntryFlow.collectAsState(initial = null).value?.destination?.route ?: ""

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            QingLongDrawer(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route) {
                        popUpTo(Screen.Task.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onSwitchServer = {
                    scope.launch { drawerState.close() }
                    // 不调 logout（保留token），只是跳回登录页让用户选服务器
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
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
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    }
                )
            }

            // Placeholder routes — to be implemented in V1.0+
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
    }
}
