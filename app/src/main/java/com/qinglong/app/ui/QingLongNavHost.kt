package com.qinglong.app.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.withContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.qinglong.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.qinglong.app.ui.components.QingLongDrawer
import com.qinglong.app.ui.components.ServerListDialog
import com.qinglong.app.ui.navigation.Screen
import com.qinglong.app.ui.screens.config.ConfigScreen
import com.qinglong.app.ui.screens.env.EnvVariableScreen
import com.qinglong.app.ui.screens.login.LoginScreen
import com.qinglong.app.ui.screens.login.LoginViewModel
import com.qinglong.app.ui.screens.panel.PanelSettingsScreen
import com.qinglong.app.ui.screens.security.SecuritySettingsScreen
import com.qinglong.app.ui.screens.appmanagement.AppManagementScreen
import com.qinglong.app.ui.screens.notification.NotificationScreen
import com.qinglong.app.ui.screens.appsettings.AppSettingsScreen
import com.qinglong.app.ui.screens.dependencesetting.DependenceSettingsScreen
import com.qinglong.app.ui.screens.othersetting.OtherSettingsScreen
import com.qinglong.app.ui.screens.script.ScriptScreen
import com.qinglong.app.ui.screens.splash.SplashScreen
import com.qinglong.app.ui.screens.subscription.SubscriptionScreen
import com.qinglong.app.ui.screens.task.TaskScreen
import com.qinglong.app.ui.screens.systemlog.SystemLogScreen
import com.qinglong.app.ui.screens.loginlog.LoginLogScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QingLongNavHost() {
    val navController = rememberNavController()
    val authViewModel: LoginViewModel = hiltViewModel()
    val currentRoute = navController.currentBackStackEntryFlow.collectAsState(initial = null).value?.destination?.route ?: ""

    // Drawer 状态
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun toggleDrawer() {
        scope.launch {
            if (drawerState.isClosed) drawerState.open() else drawerState.close()
        }
    }

    // 获取服务器信息和版本信息
    var serverAddress by remember { mutableStateOf("") }
    var qinglongVersion by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val server = authViewModel.getServerConfig()
        if (server != null) {
            serverAddress = "${server.protocol}://${server.domain}:${server.port}"
            username = server.username
        }

        // 等待 API 实例就绪后获取版本号
        var retries = 0
        while (retries < 50) { // 最多等 5 秒
            val version = withContext(kotlinx.coroutines.Dispatchers.IO) {
                authViewModel.getQinglongVersion()
            }
            if (version != null && version.isNotBlank()) {
                qinglongVersion = version
                break
            }
            delay(100)
            retries++
        }
    }

    // 服务器列表弹窗状态
    var showDonateDialog by remember { mutableStateOf(false) }
    var showServerList by remember { mutableStateOf(false) }
    var savedServers by remember { mutableStateOf(emptyList<com.qinglong.app.data.model.ServerConfig>()) }

    // 捐赠弹窗
    if (showDonateDialog) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { showDonateDialog = false },
            title = {
                Text(
                    text = "支持开发者",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "如果这个 APP 对你有帮助，欢迎捐赠支持！",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))

                    // 二维码 1
                    DonateQrCard(
                        drawableRes = R.drawable.donate_qr1,
                        label = "微信",
                        context = context,
                        scope = scope
                    )
                    Spacer(Modifier.height(20.dp))

                    // 二维码 2
                    DonateQrCard(
                        drawableRes = R.drawable.donate_qr2,
                        label = "支付宝",
                        context = context,
                        scope = scope
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDonateDialog = false }) { Text("关闭") }
            }
        )
    }

    // 服务器列表弹窗
    if (showServerList) {
        ServerListDialog(
            servers = savedServers,
            onSelect = { server ->
                showServerList = false
                authViewModel.selectServerAndLogin(server)
                // 登录成功后刷新 Drawer 信息
                scope.launch {
                    val s = authViewModel.getServerConfig()
                    if (s != null) {
                        serverAddress = "${s.protocol}://${s.domain}:${s.port}"
                        username = s.username
                    }
                    val v = authViewModel.getQinglongVersion()
                    if (v != null) qinglongVersion = v
                }
            },
            onEdit = { server ->
                authViewModel.showEditServer(server)
            },
            onDelete = { server ->
                authViewModel.requestDeleteServer(server)
            },
            onDismiss = { showServerList = false }
        )
    }

    // 编辑服务器弹窗
    val loginUiState by authViewModel.uiState.collectAsState()
    if (loginUiState.showEditDialog && loginUiState.editingServer != null) {
        // 编辑服务器弹窗（复用 LoginScreen 中的逻辑）
        var newProtocol by remember(loginUiState.editingServer) { mutableStateOf(loginUiState.editingServer!!.protocol) }
        var newDomain by remember(loginUiState.editingServer) { mutableStateOf(loginUiState.editingServer!!.domain) }
        var newPort by remember(loginUiState.editingServer) { mutableStateOf(loginUiState.editingServer!!.port.toString()) }
        var newUsername by remember(loginUiState.editingServer) { mutableStateOf(loginUiState.editingServer!!.username) }
        var newPassword by remember { mutableStateOf(loginUiState.editingPassword) }

        AlertDialog(
            onDismissRequest = { authViewModel.hideEditServer() },
            title = { Text("编辑服务器") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 协议选择
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = newProtocol == "http",
                            onClick = { newProtocol = "http" },
                            label = { Text("HTTP") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = newProtocol == "https",
                            onClick = { newProtocol = "https" },
                            label = { Text("HTTPS") }
                        )
                    }
                    OutlinedTextField(
                        value = newDomain,
                        onValueChange = { newDomain = it },
                        label = { Text("域名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPort,
                        onValueChange = { newPort = it },
                        label = { Text("端口") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("用户名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("密码（留空不修改）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    authViewModel.confirmEditServer(
                        loginUiState.editingServer!!,
                        newProtocol,
                        newDomain,
                        newPort.toIntOrNull() ?: 5700,
                        newUsername,
                        newPassword
                    )
                    // 刷新服务器列表
                    savedServers = authViewModel.getServerConfig()?.let { listOf(it) } ?: emptyList()
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { authViewModel.hideEditServer() }) { Text("取消") }
            }
        )
    }

    // 删除确认弹窗
    if (loginUiState.showDeleteConfirm && loginUiState.editingServer != null) {
        AlertDialog(
            onDismissRequest = { authViewModel.cancelDelete() },
            title = { Text("确认删除") },
            text = { Text("确定要删除服务器「${loginUiState.editingServer!!.domain}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    authViewModel.deleteServer(loginUiState.editingServer!!)
                    savedServers = authViewModel.getServerConfig()?.let { listOf(it) } ?: emptyList()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { authViewModel.cancelDelete() }) { Text("取消") }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            QingLongDrawer(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Task.route) { saveState = true }
                        launchSingleTop = true
                        // 面板设置每次都从首页开始，不恢复上次的滚动位置
                        restoreState = route != Screen.PanelSettings.route
                    }
                    // 关闭导航抽屉
                    scope.launch { drawerState.close() }
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onDonate = {
                    showDonateDialog = true
                },
                onSwitchServer = {
                    // 弹出服务器列表弹窗
                    savedServers = authViewModel.getServerConfig()?.let { listOf(it) } ?: emptyList()
                    showServerList = true
                },
                username = username,
                serverAddress = serverAddress,
                qinglongVersion = qinglongVersion
            )
        }
    ) {
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
                    onMenuClick = { toggleDrawer() }
                )
            }

            composable(Screen.Subscription.route) {
                SubscriptionScreen(
                    onMenuClick = { toggleDrawer() }
                )
            }

            composable(Screen.EnvVar.route) {
                EnvVariableScreen(
                    onMenuClick = { toggleDrawer() }
                )
            }

            composable(Screen.Script.route) {
                ScriptScreen(
                    onMenuClick = { toggleDrawer() }
                )
            }

            composable(Screen.Dependence.route) {
                com.qinglong.app.ui.screens.dependence.DependenceScreen(
                    onMenuClick = { toggleDrawer() }
                )
            }

            composable(Screen.Notification.route) {
                NotificationScreen(
                    onMenuClick = { toggleDrawer() }
                )
            }

            composable(Screen.PanelSettings.route) {
                PanelSettingsScreen(
                    onMenuClick = { toggleDrawer() },
                    navController = navController
                )
            }

            composable(Screen.SecuritySettings.route) {
                SecuritySettingsScreen(
                    onMenuClick = { toggleDrawer() }
                )
            }

            composable(Screen.AppManagement.route) {
                AppManagementScreen(
                    onMenuClick = { toggleDrawer() }
                )
            }

            composable(Screen.Config.route) {
                ConfigScreen(
                    onMenuClick = { toggleDrawer() }
                )
            }

            composable(Screen.DependenceSettings.route) {
                DependenceSettingsScreen(
                    onMenuClick = { toggleDrawer() }
                )
            }

            composable(Screen.OtherSettings.route) {
                OtherSettingsScreen(
                    onMenuClick = { toggleDrawer() }
                )
            }

            composable(Screen.SystemLog.route) {
                SystemLogScreen(
                    onMenuClick = { toggleDrawer() }
                )
            }

            composable(Screen.LoginLog.route) {
                LoginLogScreen(
                    onMenuClick = { toggleDrawer() }
                )
            }

            composable(Screen.AppSettings.route) {
                AppSettingsScreen(
                    onMenuClick = { toggleDrawer() },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

// ===================== 捐赠弹窗子组件 =====================

@Composable
private fun DonateQrCard(
    drawableRes: Int,
    label: String,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 平台标识
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // 二维码
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = "$label 收款码",
            modifier = Modifier.size(200.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )

        Spacer(Modifier.height(6.dp))

        // 保存图片按钮
        OutlinedButton(
            onClick = {
                scope.launch {
                    saveDrawableToGallery(context, drawableRes, label)
                }
            },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("保存图片", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * 将 Drawable 资源保存到系统相册（Android 10+ 不需要存储权限）
 */
private suspend fun saveDrawableToGallery(context: Context, drawableRes: Int, label: String) {
    withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeResource(context.resources, drawableRes)
            val filename = "qinglong_donate_${label}_${System.currentTimeMillis()}.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "$label 收款码已保存到相册", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "保存失败，请稍后重试", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
