package com.qinglong.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val icon: ImageVector
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
        NavItem("task", "定时任务", Icons.Default.Schedule),
        NavItem("subscription", "订阅管理", Icons.Default.Subscriptions),
        NavItem("log", "日志管理", Icons.Default.Article),
        NavItem("envvar", "环境变量", Icons.Default.Code),
        NavItem("system", "系统状态", Icons.Default.Info),
        NavItem("panel_settings", "面板设置", Icons.Default.Settings),
        NavItem("app_settings", "应用设置", Icons.Default.PhoneAndroid)
    )

    ModalDrawerSheet(modifier = modifier.width(300.dp)) {
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
            Column {
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "青龙面板",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Divider(
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

        // Logout
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )

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
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("搜索任务名称...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "清除")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = QingLongGreen,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

// ===================== BatchActionBar =====================

@Composable
fun BatchActionBar(
    selectedCount: Int,
    onEnableAll: () -> Unit,
    onDisableAll: () -> Unit,
    onDeleteAll: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "已选 $selectedCount 项",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalButton(
                    onClick = onEnableAll,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = QingLongGreen.copy(alpha = 0.15f)
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("启用", style = MaterialTheme.typography.labelSmall)
                }
                FilledTonalButton(
                    onClick = onDisableAll,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = StatusWarning.copy(alpha = 0.15f)
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("禁用", style = MaterialTheme.typography.labelSmall)
                }
                FilledTonalButton(
                    onClick = onDeleteAll,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = StatusFailed.copy(alpha = 0.15f)
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("删除", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
