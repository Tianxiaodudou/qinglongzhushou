package com.qinglong.app.ui.screens.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qinglong.app.ui.components.CodeEditor
import com.qinglong.app.ui.components.QingLongTopBar
import com.qinglong.app.ui.components.getLanguageByFilename

/**
 * 配置文件页面
 * 对应青龙面板网页端 src/pages/config/index.tsx
 * 使用公共 CodeEditor 组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun ConfigScreen(
    onMenuClick: () -> Unit,
    viewModel: ConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var textFieldValue by remember(uiState.content) {
        mutableStateOf(TextFieldValue(uiState.content))
    }
    var showFileDropdown by remember { mutableStateOf(false) }
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.3f &&
        MaterialTheme.colorScheme.background.green < 0.3f &&
        MaterialTheme.colorScheme.background.blue < 0.3f

    // 内容变化时同步到 ViewModel
    LaunchedEffect(uiState.content) {
        if (textFieldValue.text != uiState.content) {
            textFieldValue = TextFieldValue(uiState.content)
        }
    }

    // Toast
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // 根据文件扩展名推断语言
    val language = remember(uiState.selectedFile) {
        getLanguageByFilename(uiState.selectedFile)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶栏：标题 + 文件下拉选择器 + 保存按钮
            QingLongTopBar(
                title = uiState.selectedFile,
                onMenuClick = onMenuClick,
                actions = {
                    // 文件选择下拉菜单（类似网页端的 TreeSelect）
                    Box {
                        IconButton(onClick = { showFileDropdown = true }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "选择文件")
                        }
                        DropdownMenu(
                            expanded = showFileDropdown,
                            onDismissRequest = { showFileDropdown = false },
                            modifier = Modifier.width(240.dp)
                        ) {
                            if (uiState.files.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("暂无配置文件") },
                                    onClick = { showFileDropdown = false }
                                )
                            } else {
                                uiState.files.forEach { file ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Description,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = if (file.value == uiState.selectedFile)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Text(
                                                    text = file.title,
                                                    fontWeight = if (file.value == uiState.selectedFile)
                                                        FontWeight.Bold else FontWeight.Normal,
                                                    color = if (file.value == uiState.selectedFile)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.onSurface
                                                )
                                                if (file.value == uiState.selectedFile) {
                                                    Spacer(Modifier.width(8.dp))
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            showFileDropdown = false
                                            if (file.value != uiState.selectedFile) {
                                                viewModel.selectFile(file.value)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 保存按钮
                    IconButton(
                        onClick = { viewModel.saveConfig() },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "保存")
                        }
                    }
                }
            )

            if (uiState.isLoading && uiState.content.isEmpty()) {
                // 首次加载
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null && uiState.content.isEmpty()) {
                // 错误提示
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "加载失败",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadFiles() }) {
                            Text("重试")
                        }
                    }
                }
            } else {
                // 使用公共 CodeEditor 组件
                CodeEditor(
                    textFieldValue = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        viewModel.updateContent(newValue.text)
                    },
                    language = language,
                    isDarkTheme = isDarkTheme,
                    placeholder = "// 在此编辑配置文件...",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
