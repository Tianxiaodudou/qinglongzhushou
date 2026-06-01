package com.qinglong.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qinglong.app.data.model.CronLogFile
import com.qinglong.app.data.model.Subscription
import com.qinglong.app.data.model.Task
import com.qinglong.app.ui.theme.*

// ===================== TopAppBar =====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QingLongTopBar(
    title: String,
    onMenuClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "导航菜单"
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

// ===================== Navigation Drawer =====================

data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun QingLongDrawer(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    onDonate: () -> Unit = {},
    onSwitchServer: () -> Unit = {},
    username: String = "Admin",
    serverAddress: String = "",
    qinglongVersion: String = "",
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        NavItem("task", "定时任务", Icons.Default.Schedule),
        NavItem("subscription", "订阅管理", Icons.Default.Subscriptions),
        NavItem("envvar", "环境变量", Icons.Default.Code),
        NavItem("dependence", "依赖管理", Icons.Default.Extension),
        NavItem("script", "脚本管理", Icons.Default.Description),
        NavItem("config", "配置文件", Icons.Default.Settings),
        NavItem("panel_settings", "系统设置", Icons.Default.AdminPanelSettings),
        NavItem("app_settings", "应用设置", Icons.Default.PhoneAndroid)
    )

    ModalDrawerSheet(modifier = modifier.width(300.dp)) {
        Spacer(modifier = Modifier.height(24.dp))

        // User Header — 显示服务器信息
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(QingLongGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = QingLongGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username.ifBlank { "Admin" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (serverAddress.isNotBlank()) {
                    Text(
                        text = serverAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (qinglongVersion.isNotBlank()) {
                    Text(
                        text = "青龙面板 $qinglongVersion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Navigation Items
        navItems.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (isSelected) QingLongGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) QingLongGreen else MaterialTheme.colorScheme.onSurface
                    )
                },
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                modifier = Modifier.padding(horizontal = 12.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = QingLongGreen.copy(alpha = 0.08f),
                    unselectedContainerColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Switch Server
        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            label = {
                Text("切换服务器", color = MaterialTheme.colorScheme.primary)
            },
            selected = false,
            onClick = {
                onSwitchServer()
            },
            modifier = Modifier.padding(horizontal = 12.dp),
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = QingLongGreen.copy(alpha = 0.1f),
                unselectedContainerColor = Color.Transparent
            )
        )

        // Logout + Donate
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        tint = StatusFailed
                    )
                },
                label = {
                    Text(
                        text = "退出登录",
                        color = StatusFailed
                    )
                },
                selected = false,
                onClick = onLogout,
                modifier = Modifier
                    .weight(1f)
                    .padding(0.dp)
            )
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF6B35)
                    )
                },
                label = {
                    Text(
                        text = "捐赠",
                        color = Color(0xFFFF6B35)
                    )
                },
                selected = false,
                onClick = onDonate,
                modifier = Modifier
                    .weight(1f)
                    .padding(0.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ===================== Loading / Error / Empty =====================

@Composable
fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = QingLongGreen)
    }
}

/**
 * 加载动画（不断走动的进度条，和启动动画的 LinearProgressIndicator 风格一致）
 */
@Composable
fun LoadingAnimation() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LinearProgressIndicator(
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = QingLongGreen,
                trackColor = QingLongGreen.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "加载中...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = StatusFailed
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = QingLongGreen)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("重试")
            }
        }
    }
}

@Composable
fun EmptyView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ===================== SearchBar =====================

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit = {},
    searchMode: Int = 0,
    onSearchModeChange: (Int) -> Unit = {}
) {
    val modeLabels = listOf("按名称", "按订阅", "按标签")
    val modePlaceholders = listOf("搜索任务名称...", "搜索订阅名称...", "输入标签关键词...")

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        // 搜索模式切换
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            modeLabels.forEachIndexed { index, label ->
                TextButton(
                    onClick = { onSearchModeChange(index) },
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        color = if (searchMode == index) QingLongGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (searchMode == index) FontWeight.Bold else FontWeight.Normal
                    )
                }
                if (index < modeLabels.size - 1) {
                    Text(
                        text = "|",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 12.sp
                    )
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(modePlaceholders[searchMode]) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        // 手动搜索按钮
                        IconButton(onClick = onSearch) {
                            Icon(Icons.Default.Search, contentDescription = "搜索", tint = QingLongGreen)
                        }
                        // 清除按钮
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { onSearch() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = QingLongGreen,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

// ===================== BatchActionBar =====================

@Composable
fun BatchActionBar(
    selectedCount: Int,
    onRunAll: () -> Unit,
    onStopAll: () -> Unit,
    onEnableAll: () -> Unit,
    onDisableAll: () -> Unit,
    onPinAll: () -> Unit,
    onUnpinAll: () -> Unit,
    onDeleteAll: () -> Unit,
    onSelectAll: () -> Unit = {},
    onInvertSelection: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // 选中数量
            Text(
                text = "已选 $selectedCount 项",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // 第一行：全选 | 反选 | 运行 | 停止 | 启用 | 禁用
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SmallFilledTonalButton(
                    onClick = onSelectAll,
                    color = QingLongGreen
                ) {
                    Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("全选", style = MaterialTheme.typography.labelSmall)
                }
                SmallFilledTonalButton(
                    onClick = onInvertSelection,
                    color = StatusWarning
                ) {
                    Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("反选", style = MaterialTheme.typography.labelSmall)
                }
                SmallFilledTonalButton(
                    onClick = onRunAll,
                    color = QingLongGreen
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("运行", style = MaterialTheme.typography.labelSmall)
                }
                SmallFilledTonalButton(
                    onClick = onStopAll,
                    color = StatusWarning
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("停止", style = MaterialTheme.typography.labelSmall)
                }
                SmallFilledTonalButton(
                    onClick = onEnableAll,
                    color = QingLongGreen
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("启用", style = MaterialTheme.typography.labelSmall)
                }
                SmallFilledTonalButton(
                    onClick = onDisableAll,
                    color = StatusWarning
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("禁用", style = MaterialTheme.typography.labelSmall)
                }
            }
            // 第二行：置顶 | 取消置顶 | 删除
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SmallFilledTonalButton(
                    onClick = onPinAll,
                    color = QingLongGreen
                ) {
                    Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("置顶", style = MaterialTheme.typography.labelSmall)
                }
                SmallFilledTonalButton(
                    onClick = onUnpinAll,
                    color = StatusWarning
                ) {
                    Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("取消置顶", style = MaterialTheme.typography.labelSmall)
                }
                SmallFilledTonalButton(
                    onClick = onDeleteAll,
                    color = StatusFailed
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("删除", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SmallFilledTonalButton(
    onClick: () -> Unit,
    color: Color,
    content: @Composable RowScope.() -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        content()
    }
}

// ===================== Server List Dialog =====================

@Composable
fun ServerListDialog(
    servers: List<com.qinglong.app.data.model.ServerConfig>,
    onSelect: (com.qinglong.app.data.model.ServerConfig) -> Unit,
    onEdit: (com.qinglong.app.data.model.ServerConfig) -> Unit,
    onDelete: (com.qinglong.app.data.model.ServerConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var menuExpandedFor by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("已保存的服务器") },
        text = {
            if (servers.isEmpty()) {
                Text("暂无保存的服务器", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column {
                    servers.forEach { server ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onSelect(server) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(server.domain, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${server.protocol}://${server.domain}:${server.port} · ${server.username}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box {
                                    IconButton(onClick = { menuExpandedFor = server.id }) {
                                        Icon(Icons.Default.MoreVert, "更多", Modifier.size(20.dp))
                                    }
                                    DropdownMenu(
                                        expanded = menuExpandedFor == server.id,
                                        onDismissRequest = { menuExpandedFor = null }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("编辑") },
                                            onClick = {
                                                menuExpandedFor = null
                                                onEdit(server)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                menuExpandedFor = null
                                                onDelete(server)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
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

// ===================== 日志列表弹窗 =====================

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun TaskLogListDialog(
    task: Task,
    logFiles: List<CronLogFile>,
    isLoading: Boolean,
    isBatchMode: Boolean,
    selectedFiles: Set<String>,
    onDismiss: () -> Unit,
    onSelectFile: (CronLogFile) -> Unit,
    onDeleteFile: (CronLogFile) -> Unit = {},
    onToggleBatchMode: () -> Unit,
    onToggleFile: (CronLogFile) -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    // 返回键退出多选模式
    BackHandler(enabled = isBatchMode) {
        onToggleBatchMode()
    }

    AlertDialog(
        onDismissRequest = {
            if (isBatchMode) {
                onToggleBatchMode()
            } else {
                onDismiss()
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isBatchMode) {
                    // 多选模式：全选 / 反选 / 删除
                    IconButton(onClick = onSelectAll, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.SelectAll, contentDescription = "全选", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onInvertSelection, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.SwapVert, contentDescription = "反选", modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "已选 ${selectedFiles.size} 项",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    // 删除按钮（有选中项才可点击）
                    FilledIconButton(
                        onClick = onDeleteSelected,
                        modifier = Modifier.size(36.dp),
                        enabled = selectedFiles.isNotEmpty(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "删除选中", modifier = Modifier.size(20.dp))
                    }
                    // 退出多选
                    IconButton(onClick = onToggleBatchMode, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "退出多选", modifier = Modifier.size(20.dp))
                    }
                } else {
                    Text(
                        text = "日志: ${task.name}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // 多选入口按钮
                    if (logFiles.isNotEmpty()) {
                        IconButton(onClick = onToggleBatchMode, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Checklist, contentDescription = "多选", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("加载中...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    logFiles.isEmpty() -> {
                        Text(
                            text = "暂无历史日志",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(logFiles) { file ->
                                val dateStr = try {
                                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                    sdf.format(java.util.Date(file.time.toLong()))
                                } catch (e: Exception) {
                                    file.filename
                                }
                                val isSelected = file.fullPath in selectedFiles
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                if (isBatchMode) {
                                                    onToggleFile(file)
                                                } else {
                                                    onSelectFile(file)
                                                }
                                            },
                                            onLongClick = {
                                                if (!isBatchMode) {
                                                    onDeleteFile(file)
                                                }
                                            }
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 多选模式下的复选框
                                        if (isBatchMode) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { onToggleFile(file) },
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = dateStr,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = file.filename,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (!isBatchMode) {
                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = "查看",
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isBatchMode) {
                // 多选模式下底部显示操作提示
                Text(
                    text = "按返回键退出多选",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    )
}

// ===================== 日志详情弹窗 =====================

/**
 * 日志内容详情弹窗（运行中实时日志 / 历史日志详情）
 */
@Composable
fun TaskLogDetailDialog(
    task: Task,
    logContent: String,
    isLoading: Boolean,
    title: String? = null,
    autoRefreshEnabled: Boolean = true,
    onRefresh: (() -> Unit)? = null,
    onToggleAutoRefresh: ((Boolean) -> Unit)? = null,
    onDismiss: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", modifier = Modifier.size(20.dp))
                    }
                }
                Text(
                    text = title ?: (if (task.isRunning) "实时日志: ${task.name}" else "日志: ${task.name}"),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.isRunning) {
                    // 手动刷新按钮
                    IconButton(onClick = { onRefresh?.invoke() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新", modifier = Modifier.size(20.dp))
                    }
                    // 自动刷新开关
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = autoRefreshEnabled,
                            onCheckedChange = { onToggleAutoRefresh?.invoke(it) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("加载中...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    logContent.startsWith("加载失败") -> {
                        Text(
                            text = logContent,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    logContent.isBlank() -> {
                        Text(
                            text = "暂无日志",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        val scrollState = rememberScrollState()
                        // 日志内容变化时自动滚动到底部（瞬间滚动，无动画，避免弹窗跳动）
                        LaunchedEffect(logContent) {
                            scrollState.scrollTo(scrollState.maxValue)
                        }
                        SelectionContainer {
                            Text(
                                text = logContent,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(scrollState)
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

// ===================== Subscription Dialog (创建/编辑共用) =====================

/**
 * 订阅创建/编辑弹窗
 * @param isCreate true=新建模式，false=编辑模式
 * @param subscription 编辑模式时传入现有订阅数据
 * @param onDismiss 关闭弹窗
 * @param onSave 保存回调，参数为 body Map
 */
@Composable
fun SubscriptionDialog(
    isCreate: Boolean,
    subscription: Subscription? = null,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any>) -> Unit
) {
    // 初始值：创建模式用默认值，编辑模式从 subscription 读取
    var name by remember { mutableStateOf(if (isCreate) "" else subscription?.name ?: "") }
    var url by remember { mutableStateOf(if (isCreate) "" else subscription?.url ?: "") }
    var type by remember { mutableStateOf(if (isCreate) "public-repo" else subscription?.type?.ifBlank { "public-repo" } ?: "public-repo") }
    var scheduleType by remember { mutableStateOf(if (isCreate) "cron" else subscription?.schedule_type ?: "cron") }
    var schedule by remember { mutableStateOf(if (isCreate) "" else subscription?.schedule ?: "") }
    var intervalValue by remember {
        mutableStateOf(
            if (isCreate) "100"
            else subscription?.interval_schedule?.get("value")?.toString() ?: "100"
        )
    }
    var intervalUnit by remember {
        mutableStateOf(
            if (isCreate) "days"
            else subscription?.interval_schedule?.get("type") as? String ?: "days"
        )
    }
    var whitelist by remember { mutableStateOf(if (isCreate) "" else subscription?.whitelist ?: "") }
    var blacklist by remember { mutableStateOf(if (isCreate) "" else subscription?.blacklist ?: "") }
    var extensions by remember { mutableStateOf(if (isCreate) "js sh" else subscription?.extensions ?: "js sh") }
    var branch by remember { mutableStateOf(if (isCreate) "" else subscription?.branch ?: "") }
    var dependences by remember { mutableStateOf(if (isCreate) "" else subscription?.dependences ?: "") }
    var proxy by remember { mutableStateOf(if (isCreate) "" else subscription?.proxy ?: "") }
    var autoAddCron by remember { mutableStateOf(if (isCreate) true else subscription?.autoAddCron == 1) }
    var autoDelCron by remember { mutableStateOf(if (isCreate) true else subscription?.autoDelCron == 1) }
    var subBefore by remember { mutableStateOf(if (isCreate) "" else subscription?.sub_before ?: "") }
    var subAfter by remember { mutableStateOf(if (isCreate) "" else subscription?.sub_after ?: "") }
    var privateKey by remember { mutableStateOf("") }

    // 唯一值：创建模式自动生成，编辑模式从 subscription 读取
    val uniqueValue = remember(name, url, branch, subscription) {
        if (isCreate) {
            if (name.isNotBlank() && url.isNotBlank()) {
                val repoName = url.substringAfterLast("/").removeSuffix(".git")
                if (branch.isNotBlank()) "${repoName}_${branch}" else repoName
            } else ""
        } else {
            subscription?.alias ?: subscription?.name ?: ""
        }
    }

    // 解析 ql repo 命令：ql repo <仓库地址> <白名单> <黑名单> <依赖文件> <分支> [文件后缀]
    // 和青龙面板官方源码一致：按空格分割，去掉引号
    fun parseQlRepoCommand(cmd: String): List<String> {
        val result = mutableListOf<String>()
        // 按空格分割，去掉引号
        val parts = cmd.split(" ").map { it.trim().replace("\"", "").replace("'", "") }
        // parts[0] = "ql", parts[1] = "repo" 或 "raw"
        result.add(parts[0]) // "ql"
        if (parts.size > 1) result.add(parts[1]) // "repo" 或 "raw"
        if (parts.size > 2) result.add(parts[2]) // 仓库地址
        if (parts.size > 3) result.add(parts[3]) // 白名单
        if (parts.size > 4) result.add(parts[4]) // 黑名单
        if (parts.size > 5) result.add(parts[5]) // 依赖文件
        if (parts.size > 6) result.add(parts[6]) // 分支
        if (parts.size > 7) result.add(parts[7]) // 文件后缀
        return result
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCreate) "新建订阅" else "编辑订阅") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { newValue ->
                        name = newValue
                        // 解析 ql repo 命令自动填充字段
                        if (newValue.startsWith("ql ")) {
                            val parts = parseQlRepoCommand(newValue)
                            // parts[0]="ql", parts[1]="repo"/"raw", parts[2]=仓库地址, parts[3]=白名单, parts[4]=黑名单, parts[5]=依赖文件, parts[6]=分支, parts[7]=文件后缀
                            if (parts.size >= 3) url = parts[2]
                            if (parts.size >= 4 && parts[3].isNotBlank()) whitelist = parts[3]
                            if (parts.size >= 5 && parts[4].isNotBlank()) blacklist = parts[4]
                            if (parts.size >= 6 && parts[5].isNotBlank()) dependences = parts[5]
                            if (parts.size >= 7 && parts[6].isNotBlank()) branch = parts[6]
                        }
                    },
                    label = { Text("* 名称") },
                    placeholder = { Text("Supports copying ql repo/raw command for import") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // 类型（FilterChip 三选一）
                Text("* 类型", fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = type == "public-repo",
                        onClick = { type = "public-repo" },
                        label = { Text("公开仓库") }
                    )
                    Spacer(Modifier.width(4.dp))
                    FilterChip(
                        selected = type == "private-repo",
                        onClick = { type = "private-repo" },
                        label = { Text("私有仓库") }
                    )
                    Spacer(Modifier.width(4.dp))
                    FilterChip(
                        selected = type == "single-file",
                        onClick = { type = "single-file" },
                        label = { Text("单文件") }
                    )
                }
                // 链接（多行文本框）
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("* 链接") },
                    placeholder = { Text("仓库地址或文件链接") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                // Private Key（仅私有仓库显示）
                if (type == "private-repo") {
                    OutlinedTextField(
                        value = privateKey,
                        onValueChange = { privateKey = it },
                        label = { Text("Private Key") },
                        placeholder = { Text("Private key content") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // 唯一值（只读，自动生成）
                OutlinedTextField(
                    value = uniqueValue,
                    onValueChange = {},
                    label = { Text("* 唯一值") },
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // 定时规则类型
                Text("* 定时规则", fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = scheduleType == "cron",
                        onClick = { scheduleType = "cron" },
                        label = { Text("Cron 表达式") }
                    )
                    Spacer(Modifier.width(4.dp))
                    FilterChip(
                        selected = scheduleType == "interval",
                        onClick = { scheduleType = "interval" },
                        label = { Text("间隔") }
                    )
                }
                if (scheduleType == "cron") {
                    OutlinedTextField(
                        value = schedule,
                        onValueChange = { schedule = it },
                        label = { Text("Cron 表达式") },
                        placeholder = { Text("Seconds (optional) Minutes Hours Day Month Week") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = intervalValue,
                            onValueChange = { intervalValue = it },
                            label = { Text("间隔值") },
                            placeholder = { Text("100") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = intervalUnit == "minutes",
                            onClick = { intervalUnit = "minutes" },
                            label = { Text("分钟") }
                        )
                        Spacer(Modifier.width(4.dp))
                        FilterChip(
                            selected = intervalUnit == "hours",
                            onClick = { intervalUnit = "hours" },
                            label = { Text("小时") }
                        )
                        Spacer(Modifier.width(4.dp))
                        FilterChip(
                            selected = intervalUnit == "days",
                            onClick = { intervalUnit = "days" },
                            label = { Text("天") }
                        )
                    }
                }
                // 分支
                OutlinedTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = { Text("分支") },
                    placeholder = { Text("Branch name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // 白名单
                OutlinedTextField(
                    value = whitelist,
                    onValueChange = { whitelist = it },
                    label = { Text("白名单") },
                    placeholder = { Text("Whitelist, one per line") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                // 黑名单
                OutlinedTextField(
                    value = blacklist,
                    onValueChange = { blacklist = it },
                    label = { Text("黑名单") },
                    placeholder = { Text("Blacklist, one per line") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                // 依赖文件
                OutlinedTextField(
                    value = dependences,
                    onValueChange = { dependences = it },
                    label = { Text("依赖文件") },
                    placeholder = { Text("Dependency files, one per line") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                // 文件扩展名
                OutlinedTextField(
                    value = extensions,
                    onValueChange = { extensions = it },
                    label = { Text("文件扩展名") },
                    placeholder = { Text("File extensions, e.g. js py ts") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // 代理
                OutlinedTextField(
                    value = proxy,
                    onValueChange = { proxy = it },
                    label = { Text("代理") },
                    placeholder = { Text("SOCK5代理，例如 IP:PORT") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // 执行前命令
                OutlinedTextField(
                    value = subBefore,
                    onValueChange = { subBefore = it },
                    label = { Text("执行前命令") },
                    placeholder = { Text("Commands to run before subscription, one per line") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                // 执行后命令
                OutlinedTextField(
                    value = subAfter,
                    onValueChange = { subAfter = it },
                    label = { Text("执行后命令") },
                    placeholder = { Text("Commands to run after subscription, one per line") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                // 自动添加/删除定时
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("自动添加任务：", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = autoAddCron,
                        onCheckedChange = { autoAddCron = it }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("自动删除任务：", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = autoDelCron,
                        onCheckedChange = { autoDelCron = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val body = mutableMapOf<String, Any>(
                        "name" to name,
                        "url" to url,
                        "type" to type
                    )
                    if (type == "private-repo" && privateKey.isNotBlank()) {
                        body["privateKey"] = privateKey
                    }
                    if (branch.isNotBlank()) body["branch"] = branch
                    if (scheduleType == "cron") {
                        body["schedule_type"] = "cron"
                        if (schedule.isNotBlank()) body["schedule"] = schedule
                    } else if (scheduleType == "interval") {
                        body["schedule_type"] = "interval"
                        body["interval_schedule"] = mapOf("type" to intervalUnit, "value" to (intervalValue.toIntOrNull() ?: 100))
                    }
                    if (whitelist.isNotBlank()) body["whitelist"] = whitelist
                    if (blacklist.isNotBlank()) body["blacklist"] = blacklist
                    if (dependences.isNotBlank()) body["dependences"] = dependences
                    if (extensions.isNotBlank()) body["extensions"] = extensions
                    if (subBefore.isNotBlank()) body["sub_before"] = subBefore
                    if (subAfter.isNotBlank()) body["sub_after"] = subAfter
                    if (proxy.isNotBlank()) body["proxy"] = proxy
                    body["autoAddCron"] = autoAddCron
                    body["autoDelCron"] = autoDelCron
                    // alias
                    if (uniqueValue.isNotBlank()) body["alias"] = uniqueValue
                    onSave(body)
                },
                enabled = name.isNotBlank() && url.isNotBlank() && !(type == "private-repo" && privateKey.isBlank())
            ) { Text(if (isCreate) "创建" else "保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ===================== Task Create/Edit Dialogs =====================

@Composable
fun TaskCreateDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, command: String, schedule: String, labels: List<String>,
                allowMultipleInstances: Int, logName: String?, taskBefore: String?, taskAfter: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("") }
    var labelsText by remember { mutableStateOf("") }
    var allowMultipleInstances by remember { mutableStateOf(false) }
    var logName by remember { mutableStateOf("") }
    var taskBefore by remember { mutableStateOf("") }
    var taskAfter by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建任务", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("任务名称") },
                    placeholder = { Text("如: 每日签到、京东农场、test_log_100") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("命令") },
                    placeholder = { Text("如: task test_log.sh 或 ql repo https://... 或 echo hello") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text("定时规则 (Cron)") },
                    placeholder = { Text("格式: 秒(可选) 分 时 日 月 周  如: 0 0 1 1 * 或 0 */5 * * * ?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = labelsText,
                    onValueChange = { labelsText = it },
                    label = { Text("标签 (逗号分隔)") },
                    placeholder = { Text("如: 农场, 签到, 日常  用于分类和搜索") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("多实例模式", modifier = Modifier.weight(1f))
                        Switch(
                            checked = allowMultipleInstances,
                            onCheckedChange = { allowMultipleInstances = it }
                        )
                    }
                    Text(
                        text = if (allowMultipleInstances) "允许同时运行多个实例" else "同一时间只能运行一个实例（默认）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = logName,
                    onValueChange = { logName = it },
                    label = { Text("日志名称") },
                    placeholder = { Text("留空=默认目录  /dev/null=不记录日志") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = taskBefore,
                    onValueChange = { taskBefore = it },
                    label = { Text("执行前命令") },
                    placeholder = { Text("任务开始前执行的命令，如: 发送通知、检查网络等（可选）") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = taskAfter,
                    onValueChange = { taskAfter = it },
                    label = { Text("执行后命令") },
                    placeholder = { Text("任务结束后执行的命令，如: 发送完成通知、清理文件等（可选）") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedLabels = labelsText.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                onConfirm(
                    name.trim(),
                    command.trim(),
                    schedule.trim(),
                    parsedLabels,
                    if (allowMultipleInstances) 1 else 0,
                    logName.trim().ifBlank { null },
                    taskBefore.trim().ifBlank { null },
                    taskAfter.trim().ifBlank { null }
                )
            }) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun TaskEditDialog(
    task: Task,
    onDismiss: () -> Unit,
    onConfirm: (name: String, command: String, schedule: String, labels: List<String>,
                allowMultipleInstances: Int, logName: String?, taskBefore: String?, taskAfter: String?) -> Unit
) {
    var name by remember { mutableStateOf(task.name) }
    var command by remember { mutableStateOf(task.command) }
    var schedule by remember { mutableStateOf(task.schedule) }
    var labelsText by remember { mutableStateOf(task.labels?.joinToString(", ") ?: "") }
    var allowMultipleInstances by remember { mutableStateOf(task.allow_multiple_instances == 1) }
    var logName by remember { mutableStateOf(task.log_name ?: "") }
    var taskBefore by remember { mutableStateOf(task.task_before?.toString() ?: "") }
    var taskAfter by remember { mutableStateOf(task.task_after?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑任务", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("任务名称") },
                    placeholder = { Text("如: 每日签到、京东农场、test_log_100") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("命令") },
                    placeholder = { Text("如: task test_log.sh 或 ql repo https://... 或 echo hello") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text("定时规则 (Cron)") },
                    placeholder = { Text("格式: 秒(可选) 分 时 日 月 周  如: 0 0 1 1 * 或 0 */5 * * * ?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = labelsText,
                    onValueChange = { labelsText = it },
                    label = { Text("标签 (逗号分隔)") },
                    placeholder = { Text("如: 农场, 签到, 日常  用于分类和搜索") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("多实例模式", modifier = Modifier.weight(1f))
                        Switch(
                            checked = allowMultipleInstances,
                            onCheckedChange = { allowMultipleInstances = it }
                        )
                    }
                    Text(
                        text = if (allowMultipleInstances) "允许同时运行多个实例" else "同一时间只能运行一个实例（默认）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = logName,
                    onValueChange = { logName = it },
                    label = { Text("日志名称") },
                    placeholder = { Text("留空=默认目录  /dev/null=不记录日志") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = taskBefore,
                    onValueChange = { taskBefore = it },
                    label = { Text("执行前命令") },
                    placeholder = { Text("任务开始前执行的命令，如: 发送通知、检查网络等（可选）") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = taskAfter,
                    onValueChange = { taskAfter = it },
                    label = { Text("执行后命令") },
                    placeholder = { Text("任务结束后执行的命令，如: 发送完成通知、清理文件等（可选）") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedLabels = labelsText.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                onConfirm(
                    name.trim(),
                    command.trim(),
                    schedule.trim(),
                    parsedLabels,
                    if (allowMultipleInstances) 1 else 0,
                    logName.trim().ifBlank { null },
                    taskBefore.trim().ifBlank { null },
                    taskAfter.trim().ifBlank { null }
                )
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ===================== View Management Dialogs =====================

// ===================== Error Dialog =====================

/**
 * 通用错误提示弹窗
 * 显示错误标题和详细错误信息，支持复制错误信息
 */
@Composable
fun ErrorDialog(
    title: String = "错误",
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            SelectionContainer {
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

