package com.qinglong.app.ui.screens.dependencesetting

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qinglong.app.ui.components.LoadingView
import com.qinglong.app.ui.components.QingLongTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DependenceSettingsScreen(
    onMenuClick: () -> Unit,
    viewModel: DependenceSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val toastMessage by viewModel.toastMessage.collectAsState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        QingLongTopBar(
            title = "依赖设置",
            onMenuClick = onMenuClick
        )

        if (uiState.isLoading) {
            LoadingView()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 代理设置
                SettingsConfigCard(
                    title = "代理设置",
                    subtitle = "配置依赖下载代理地址",
                    value = uiState.proxy,
                    placeholder = "如: http://127.0.0.1:7890",
                    isSaving = uiState.isSaving,
                    onValueChange = { viewModel.updateProxy(it) },
                    onSave = { viewModel.saveProxy() }
                )

                // Node 镜像源
                SettingsConfigCard(
                    title = "Node 镜像源",
                    subtitle = "配置 npm/pnpm/yarn 镜像源地址",
                    value = uiState.nodeMirror,
                    placeholder = "如: https://registry.npmmirror.com",
                    isSaving = uiState.isSaving,
                    onValueChange = { viewModel.updateNodeMirror(it) },
                    onSave = { viewModel.saveNodeMirror() }
                )

                // Python 镜像源
                SettingsConfigCard(
                    title = "Python 镜像源",
                    subtitle = "配置 pip 镜像源地址",
                    value = uiState.pythonMirror,
                    placeholder = "如: https://pypi.tuna.tsinghua.edu.cn/simple",
                    isSaving = uiState.isSaving,
                    onValueChange = { viewModel.updatePythonMirror(it) },
                    onSave = { viewModel.savePythonMirror() }
                )

                // Linux 镜像源
                SettingsConfigCard(
                    title = "Linux 镜像源",
                    subtitle = "配置 apt/apk/yum 镜像源地址",
                    value = uiState.linuxMirror,
                    placeholder = "如: https://mirrors.ustc.edu.cn",
                    isSaving = uiState.isSaving,
                    onValueChange = { viewModel.updateLinuxMirror(it) },
                    onSave = { viewModel.saveLinuxMirror() }
                )

                // 清除依赖缓存
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "清除依赖缓存",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "清除 node_modules 或 pip 缓存的依赖包",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 选择类型
                            FilterChip(
                                selected = uiState.cleanType == "node",
                                onClick = { viewModel.updateCleanType("node") },
                                label = { Text("Node") },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            FilterChip(
                                selected = uiState.cleanType == "python3",
                                onClick = { viewModel.updateCleanType("python3") },
                                label = { Text("Python3") }
                            )
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = { viewModel.cleanCache() },
                                enabled = !uiState.isSaving,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("清理")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsConfigCard(
    title: String,
    subtitle: String,
    value: String,
    placeholder: String,
    isSaving: Boolean,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onSave,
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("保存")
                }
            }
        }
    }
}
