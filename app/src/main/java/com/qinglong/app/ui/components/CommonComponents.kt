package com.qinglong.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val icon: @Composable () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QingLongDrawer(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    username: String = "Admin",
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        NavItem("task", "定时任务") { Icon(Icons.Default.Schedule, null) },
        NavItem("subscription", "订阅管理") { Icon(Icons.Default.Subscriptions, null) },
        NavItem("log", "日志管理") { Icon(Icons.Default.Article, null) },
        NavItem("envvar", "环境变量") { Icon(Icons.Default.Variable, null) },
        NavItem("system", "系统状态") { Icon(Icons.Default.Monitor, null) },
        NavItem("panel_settings", "面板设置") { Icon(Icons.Default.Settings, null) },
        NavItem("app_settings", "应用设置") { Icon(Icons.Default.PhoneAndroid, null) }
    )

    ModalDrawerSheet(modifier = modifier) {
        Spacer(modifier = Modifier.height(24.dp))

        // User Header
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
                    .background(QingLongGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = username.firstOrNull()?.uppercase() ?: "A",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "青龙面板",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Nav Items
        navItems.forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = { item.icon() },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = QingLongGreen.copy(alpha = 0.12f),
                    selectedTextColor = QingLongGreen,
                    selectedIconColor = QingLongGreen
                ),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NavigationDrawerItem(
            label = { Text("退出登录", color = StatusFailed) },
            selected = false,
            onClick = onLogout,
            icon = { Icon(Icons.Default.Logout, null, tint = StatusFailed) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = StatusFailed.copy(alpha = 0.08f),
                selectedTextColor = StatusFailed,
                selectedIconColor = StatusFailed
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ===================== Task Card =====================

@Composable
fun TaskCard(
    name: String,
    schedule: String,
    isRunning: Boolean,
    isDisabled: Boolean,
    lastRunTime: String?,
    execStatus: ExecStatus?,
    onRun: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isRunning -> StatusRunning
                            isDisabled -> StatusStopped
                            execStatus == ExecStatus.SUCCESS -> StatusSuccess
                            execStatus == ExecStatus.FAILED -> StatusFailed
                            else -> StatusStopped
                        }
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDisabled) StatusStopped else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = schedule,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                lastRunTime?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "上次: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isRunning) {
                IconButton(onClick = onStop) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "停止",
                        tint = StatusFailed
                    )
                }
            } else {
                IconButton(onClick = onRun) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "运行",
                        tint = QingLongGreen
                    )
                }
            }
        }
    }
}

enum class ExecStatus { SUCCESS, FAILED }

// ===================== Loading / Error States =====================

@Composable
fun LoadingView(message: String = "加载中…") {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = QingLongGreen)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
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
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = StatusFailed,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = QingLongGreen)
            ) {
                Text("重试")
            }
        }
    }
}

@Composable
fun EmptyView(message: String = "暂无数据") {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
