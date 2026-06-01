package com.qinglong.app.ui.screens.panel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.qinglong.app.ui.components.QingLongTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelSettingsScreen(
    onMenuClick: () -> Unit,
    navController: NavController? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        QingLongTopBar(
            title = "系统设置",
            onMenuClick = onMenuClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {


            // ===== 安全与认证 =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "安全与认证",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "修改密码、两步验证（2FA）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    SettingsNavItem(
                        icon = Icons.Default.Security,
                        title = "安全设置",
                        subtitle = "修改用户名密码，管理两步验证",
                        onClick = { navController?.navigate("security_settings") }
                    )
                }
            }

            // ===== 开放平台 =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "开放平台",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "管理 OAuth 应用和 API 访问密钥",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    SettingsNavItem(
                        icon = Icons.Default.DevicesOther,
                        title = "应用管理",
                        subtitle = "创建、编辑和管理开放平台应用",
                        onClick = { navController?.navigate("app_management") }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsNavItem(
                        icon = Icons.Default.Notifications,
                        title = "通知设置",
                        subtitle = "配置消息推送通知",
                        onClick = { navController?.navigate("notification") }
                    )
                }
            }

            // ===== 日志管理 =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "日志管理",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "查看系统运行日志和登录历史",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    SettingsNavItem(
                        icon = Icons.AutoMirrored.Filled.Article,
                        title = "系统日志",
                        subtitle = "查看青龙面板运行日志",
                        onClick = { navController?.navigate("system_log") }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsNavItem(
                        icon = Icons.AutoMirrored.Filled.Login,
                        title = "登录日志",
                        subtitle = "查看登录历史记录",
                        onClick = { navController?.navigate("login_log") }
                    )
                }
            }

            // ===== 依赖设置 =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "依赖设置",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "配置镜像源和代理",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    SettingsNavItem(
                        icon = Icons.Default.Download,
                        title = "依赖设置",
                        subtitle = "配置镜像源、代理和依赖缓存清理",
                        onClick = { navController?.navigate("dependence_settings") }
                    )
                }
            }

            // ===== 其他设置 =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "其他设置",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "日志保留、并发数、时区等",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    SettingsNavItem(
                        icon = Icons.Default.Tune,
                        title = "其他设置",
                        subtitle = "日志保留、并发数、时区、SSH Key 等",
                        onClick = { navController?.navigate("other_settings") }
                    )
                }
            }

        }
    }
}

@Composable
fun SettingsNavItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
