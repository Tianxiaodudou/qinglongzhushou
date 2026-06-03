package com.qinglong.app.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .imePadding()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.fillMaxHeight(0.08f))

            // Logo area
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "青龙助手",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "登录到青龙面板",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Login Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // HTTP / HTTPS Switch (默认 HTTP)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HTTPS",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = uiState.isHttps,
                            onCheckedChange = viewModel::updateHttps,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                    // Domain
                    OutlinedTextField(
                        value = uiState.domain,
                        onValueChange = viewModel::updateDomain,
                        label = { Text("服务器域名") },
                        placeholder = { Text("yourdomain.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Language, null)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Port
                    OutlinedTextField(
                        value = uiState.port,
                        onValueChange = viewModel::updatePort,
                        label = { Text("端口") },
                        placeholder = { Text("5700") },
                        leadingIcon = {
                            Icon(Icons.Default.Lan, null)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Username
                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = viewModel::updateUsername,
                        label = { Text("用户名") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, null)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Password
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = viewModel::updatePassword,
                        label = { Text("密码") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.login()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // ===== Two-factor auth code input =====
                    if (uiState.needsTwoFactor) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "两步验证",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "该面板已开启两步验证，请输入验证器应用上的 6 位验证码",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = uiState.twoFactorCode,
                            onValueChange = viewModel::updateTwoFactorCode,
                            label = { Text("验证码") },
                            placeholder = { Text("123456") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.loginWithTwoFactor()
                                }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(Modifier.height(4.dp))
                    }

                    // Error message
                    uiState.error?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Login / Verify Button
                    Button(
                        onClick = if (uiState.needsTwoFactor) viewModel::loginWithTwoFactor else viewModel::login,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (uiState.needsTwoFactor) "验证" else "登录",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Cancel 2FA and go back
                    if (uiState.needsTwoFactor) {
                        TextButton(
                            onClick = { viewModel.cancelTwoFactor() },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = "返回登录页面",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manage servers
            TextButton(onClick = { viewModel.showServerList() }) {
                Icon(
                    Icons.Default.Dns,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "管理已保存的服务器",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Server list dialog
            if (uiState.showServerDialog) {
                var menuExpandedFor by remember { mutableStateOf<String?>(null) }

                AlertDialog(
                    onDismissRequest = { viewModel.hideServerList() },
                    title = { Text("已保存的服务器") },
                    text = {
                        if (uiState.savedServers.isEmpty()) {
                            Text("暂无保存的服务器", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Column {
                                uiState.savedServers.forEach { server ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { viewModel.selectServer(server) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    if (server.name.isNotBlank() && server.name != server.domain) server.name else server.domain,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    if (server.name.isNotBlank() && server.name != server.domain) server.domain else "${server.protocol}://${server.domain}:${server.port} · ${server.username}",
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
                                                            viewModel.showEditServer(server)
                                                        },
                                                        leadingIcon = { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)) }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                                                        onClick = {
                                                            menuExpandedFor = null
                                                            viewModel.requestDeleteServer(server)
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
                        TextButton(onClick = { viewModel.hideServerList() }) {
                            Text("关闭")
                        }
                    }
                )
            }

            // Delete confirm dialog
            if (uiState.showDeleteConfirm && uiState.editingServer != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.cancelDelete() },
                    title = { Text("确认删除") },
                    text = { Text("确定要删除服务器「${uiState.editingServer!!.domain}」吗？") },
                    confirmButton = {
                        TextButton(onClick = { viewModel.deleteServer(uiState.editingServer!!) }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.cancelDelete() }) {
                            Text("取消")
                        }
                    }
                )
            }

            // Edit server dialog
            if (uiState.showEditDialog && uiState.editingServer != null) {
                val server = uiState.editingServer!!
                var editProtocol by remember { mutableStateOf(server.protocol) }
                var editDomain by remember { mutableStateOf(server.domain) }
                var editPort by remember { mutableStateOf(server.port.toString()) }
                var editUsername by remember { mutableStateOf(server.username) }
                var editPassword by remember { mutableStateOf(uiState.editingPassword) }

                AlertDialog(
                    onDismissRequest = { viewModel.hideEditServer() },
                    title = { Text("编辑服务器") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Protocol toggle
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("HTTPS", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.width(8.dp))
                                Switch(checked = editProtocol == "https", onCheckedChange = { editProtocol = if (it) "https" else "http" })
                            }
                            // Domain
                            OutlinedTextField(
                                value = editDomain,
                                onValueChange = { editDomain = it },
                                label = { Text("域名") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            // Port
                            OutlinedTextField(
                                value = editPort,
                                onValueChange = { editPort = it.filter { c -> c.isDigit() } },
                                label = { Text("端口") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            // Username
                            OutlinedTextField(
                                value = editUsername,
                                onValueChange = { editUsername = it },
                                label = { Text("账号") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            // Password
                            OutlinedTextField(
                                value = editPassword,
                                onValueChange = { editPassword = it },
                                label = { Text("密码") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val port = editPort.toIntOrNull() ?: 5700
                            viewModel.confirmEditServer(server, editProtocol, editDomain, port, editUsername, editPassword)
                        }) { Text("保存") }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.hideEditServer() }) {
                            Text("取消")
                        }
                    }
                )
            }

            // 底部留白：给键盘 + 底部安全区域
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "青龙助手 v1.5.2  ·  Android 8.0+",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
