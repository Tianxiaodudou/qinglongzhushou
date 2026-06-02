package com.qinglong.app.ui.screens.appsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qinglong.app.data.model.ServerConfig
import com.qinglong.app.ui.components.QingLongTopBar
import com.qinglong.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun AppSettingsScreen(
    onMenuClick: () -> Unit,
    onLogout: () -> Unit = {},
    viewModel: AppSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showDeleteServerConfirm by remember { mutableStateOf<ServerConfig?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        QingLongTopBar(
            title = "应用设置",
            onMenuClick = onMenuClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ===== 外观设置 =====
            AppearanceSection(
                themeMode = uiState.themeMode,
                fontScale = uiState.fontScale,
                onThemeChange = { viewModel.setThemeMode(it) },
                onFontScaleChange = { viewModel.setFontScale(it) }
            )

            // ===== 服务器管理 =====
            ServerManagementSection(
                servers = uiState.servers,
                currentServerId = uiState.currentServerId,
                onAddServer = { viewModel.showAddServer() },
                onEditServer = { viewModel.showEditServer(it) },
                onDeleteServer = { showDeleteServerConfirm = it },
                onSwitchServer = { viewModel.switchServer(it) }
            )

            // ===== 分页设置 =====
            PageSizeSection(
                title = "定时任务每页卡片数",
                pageSize = uiState.taskPageSize,
                onPageSizeChange = { viewModel.setTaskPageSize(it) }
            )
            PageSizeSection(
                title = "订阅管理每页卡片数",
                pageSize = uiState.subPageSize,
                onPageSizeChange = { viewModel.setSubPageSize(it) }
            )
            PageSizeSection(
                title = "环境变量每页卡片数",
                pageSize = uiState.envPageSize,
                onPageSizeChange = { viewModel.setEnvPageSize(it) }
            )

            // ===== 日志刷新间隔 =====
            LogRefreshSection(
                taskLogRefreshMs = uiState.taskLogRefreshMs,
                subLogRefreshMs = uiState.subLogRefreshMs,
                scriptRunRefreshMs = uiState.scriptRunRefreshMs,
                onTaskLogRefreshChange = { viewModel.setTaskLogRefreshMs(it) },
                onSubLogRefreshChange = { viewModel.setSubLogRefreshMs(it) },
                onScriptRunRefreshChange = { viewModel.setScriptRunRefreshMs(it) }
            )

            // ===== 数据管理 =====
            DataManagementSection(
                onClearData = { showClearDataConfirm = true }
            )

            // ===== 关于 =====
            AboutSection(
                appVersion = uiState.appVersion,
                qinglongVersion = uiState.qinglongVersion,
                serverAddress = uiState.serverAddress
            )

            // ===== 退出登录 =====
            Button(
                onClick = { showLogoutConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("退出登录")
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // 退出确认
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("确认退出") },
            text = { Text("确定要退出登录吗？需要重新输入账号密码才能使用。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("退出") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("取消") }
            }
        )
    }

    // 清除数据确认
    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text("确认清除全部数据") },
            text = { Text("本软件将会恢复刚安装软件时的状态，清除全部数据。此操作不可撤销，确定要继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDataConfirm = false
                        viewModel.clearAllData()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("确认清除") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) { Text("取消") }
            }
        )
    }

    // 删除服务器确认
    showDeleteServerConfirm?.let { server ->
        AlertDialog(
            onDismissRequest = { showDeleteServerConfirm = null },
            title = { Text("确认删除服务器") },
            text = { Text("确定要删除服务器「${server.name.ifBlank { server.domain }}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteServer(server)
                        showDeleteServerConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteServerConfirm = null }) { Text("取消") }
            }
        )
    }

    // 编辑/添加服务器对话框
    if (uiState.isEditingServer && uiState.editingServer != null) {
        EditServerDialog(
            server = uiState.editingServer!!,
            password = uiState.editingPassword,
            onDismiss = { viewModel.hideEditServer() },
            onSave = { s, p -> viewModel.saveServer(s, p) }
        )
    }
}

// ===================== 子组件 =====================

@Composable
private fun AppearanceSection(
    themeMode: String,
    fontScale: Int,
    onThemeChange: (String) -> Unit,
    onFontScaleChange: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("外观", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            Text("主题", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeChip("深色模式", themeMode == "dark") { onThemeChange("dark") }
                ThemeChip("浅色模式", themeMode == "light") { onThemeChange("light") }
                ThemeChip("跟随系统", themeMode == "system") { onThemeChange("system") }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("字体大小", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FontSizeSelector(fontScale, onFontScaleChange)
        }
    }
}

@Composable
private fun FontSizeSelector(scale: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FontChip("小", 85, scale == 85) { onChange(85) }
        FontChip("默认", 100, scale == 100) { onChange(100) }
        FontChip("大", 115, scale == 115) { onChange(115) }
        FontChip("超大", 130, scale == 130) { onChange(130) }
    }
}

@Composable
private fun FontChip(text: String, @Suppress("UNUSED_PARAMETER") value: Int, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) Color.Black else Color.LightGray.copy(alpha = 0.4f)
    val borderColor = if (isSelected) Color(0xFF00FF66) else Color.Gray.copy(alpha = 0.5f)
    val textColor = if (isSelected) Color.White else Color.Black
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
private fun LogRefreshSection(
    taskLogRefreshMs: Int,
    subLogRefreshMs: Int,
    scriptRunRefreshMs: Int,
    onTaskLogRefreshChange: (Int) -> Unit,
    onSubLogRefreshChange: (Int) -> Unit,
    onScriptRunRefreshChange: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("日志刷新间隔", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            LogRefreshInput("任务日志", taskLogRefreshMs, onValueChange = onTaskLogRefreshChange)
            Spacer(Modifier.height(8.dp))
            LogRefreshInput("订阅日志", subLogRefreshMs, onValueChange = onSubLogRefreshChange)
            Spacer(Modifier.height(8.dp))
            LogRefreshInput("脚本运行日志", scriptRunRefreshMs, onValueChange = onScriptRunRefreshChange)
        }
    }
}

@Composable
private fun ServerManagementSection(
    servers: List<ServerConfig>,
    currentServerId: String,
    onAddServer: () -> Unit,
    onEditServer: (ServerConfig) -> Unit,
    onDeleteServer: (ServerConfig) -> Unit,
    onSwitchServer: (ServerConfig) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("服务器管理", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                TextButton(onClick = onAddServer) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("添加")
                }
            }
            Spacer(Modifier.height(8.dp))
            if (servers.isEmpty()) {
                Text(
                    "暂无已保存的服务器",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                servers.forEachIndexed { index, server ->
                    ServerItem(
                        server = server,
                        isCurrent = server.id == currentServerId,
                        onEdit = { onEditServer(server) },
                        onDelete = { onDeleteServer(server) },
                        onSwitch = { onSwitchServer(server) }
                    )
                    if (index < servers.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerItem(
    server: ServerConfig,
    isCurrent: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSwitch: () -> Unit
) {
    val itemModifier = if (isCurrent) Modifier else Modifier.clickable { onSwitch() }
    Surface(
        modifier = itemModifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Dns,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = server.name.ifBlank { server.domain },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    )
                    if (isCurrent) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "当前",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Text(
                    text = "${server.protocol}://${server.domain}:${server.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun DataManagementSection(onClearData: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("数据管理", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onClearData,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("清除全部数据")
            }
        }
    }
}

@Composable
private fun AboutSection(
    appVersion: String,
    qinglongVersion: String,
    serverAddress: String
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("关于", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            AboutItem(Icons.Default.Info, "APP 版本", appVersion)
            if (serverAddress.isNotBlank()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                AboutItem(Icons.Default.Dns, "服务器地址", serverAddress)
            }
            if (qinglongVersion.isNotBlank()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                AboutItem(Icons.Default.Cloud, "青龙面板版本", qinglongVersion)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            AboutItem(
                Icons.Default.Code,
                "开源许可",
                "GNU GPL-3.0",
                onClick = {}
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            AboutItem(
                Icons.Default.BugReport,
                "反馈问题",
                "GitHub Issues",
                onClick = {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://github.com/Tianxiaodudou/qinglongzhushou")
                    )
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
private fun AboutItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ThemeChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) Color.Black else Color.LightGray.copy(alpha = 0.4f))
            .border(
                width = 1.5.dp,
                color = if (isSelected) Color(0xFF00FF66) else Color.Gray.copy(alpha = 0.5f),
                shape = RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color.Black,
            maxLines = 1
        )
    }
}

@Composable
private fun LogRefreshInput(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(100.dp)
        )
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { input ->
                val num = input.filter { it.isDigit() }.take(5)
                if (num.isNotEmpty()) {
                    val ms = num.toInt().coerceIn(100, 60000)
                    onValueChange(ms)
                }
            },
            placeholder = { Text("3000") },
            singleLine = true,
            modifier = Modifier.width(120.dp),
            supportingText = { Text("100 ~ 60000") }
        )
        Text(
            text = "毫秒",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EditServerDialog(
    server: ServerConfig,
    password: String,
    onDismiss: () -> Unit,
    onSave: (ServerConfig, String) -> Unit
) {
    var protocol by remember(server) { mutableStateOf(server.protocol) }
    var domain by remember(server) { mutableStateOf(server.domain) }
    var port by remember(server) { mutableStateOf(server.port.toString()) }
    var name by remember(server) { mutableStateOf(server.name) }
    var username by remember(server) { mutableStateOf(server.username) }
    var newPassword by remember { mutableStateOf(password) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (server.id.isNotEmpty() && server.domain.isNotEmpty()) "编辑服务器" else "添加服务器") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = protocol == "http",
                        onClick = { protocol = "http" },
                        label = { Text("HTTP") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = protocol == "https",
                        onClick = { protocol = "https" },
                        label = { Text("HTTPS") }
                    )
                }
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("域名/IP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() } },
                    label = { Text("端口") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
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
            TextButton(
                onClick = {
                    val updated = server.copy(
                        name = name.ifBlank { domain },
                        protocol = protocol,
                        domain = domain,
                        port = port.toIntOrNull() ?: 5700,
                        username = username
                    )
                    onSave(updated, newPassword)
                },
                enabled = domain.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun PageSizeSection(
    title: String,
    pageSize: Int,
    onPageSizeChange: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(10, 20, 30, 40, 50, 100).forEach { size ->
                    PageSizeChip(
                        text = if (size == 100) "100" else size.toString(),
                        isSelected = pageSize == size,
                        onClick = { onPageSizeChange(size) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PageSizeChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) Color.Black else Color.LightGray.copy(alpha = 0.4f))
            .border(1.5.dp, if (isSelected) Color(0xFF00FF66) else Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$text 个",
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color.Black,
            maxLines = 1
        )
    }
}
