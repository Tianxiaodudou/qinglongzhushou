package com.qinglong.app.ui.screens.script

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qinglong.app.data.model.ScriptItem
import com.qinglong.app.ui.components.QingLongTopBar
import com.qinglong.app.ui.components.cBlue
import com.qinglong.app.ui.components.cOrange
import com.qinglong.app.ui.theme.*

/**
 * 脚本文件管理器页面
 * 平铺列表样式（非树状），目录可点击进入，文件可点击编辑
 * 长按文件弹出操作选项（编辑/重命名/删除/运行/停止）
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScriptScreen(
    viewModel: ScriptViewModel = hiltViewModel(),
    onMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // 编辑器状态
    var showEditor by remember { mutableStateOf(false) }
    var editingKey by remember { mutableStateOf("") }
    var editingTitle by remember { mutableStateOf("") }
    var editingContent by remember { mutableStateOf("") }

    if (showEditor) {
        ScriptEditorScreen(
            scriptKey = editingKey,
            scriptTitle = editingTitle,
            language = getLanguageFromExtension(editingKey),
            onBack = {
                showEditor = false
            },
            onSave = { content ->
                viewModel.saveScript(editingKey, content)
                showEditor = false
            },
            viewModel = viewModel
        )
    } else {
        // 当前目录路径栈
        var pathStack by remember { mutableStateOf(listOf<String>()) }
        // 当前目录下的内容
        val currentItems = remember(uiState.scripts, pathStack) {
            if (pathStack.isEmpty()) {
                // 根目录：只显示顶级目录和文件
                uiState.scripts
            } else {
                // 进入子目录：从树中查找
                fun findChildren(items: List<ScriptItem>, path: List<String>, depth: Int): List<ScriptItem>? {
                    if (depth >= path.size) return null
                    val targetKey = path[depth]
                    for (item in items) {
                        if (item.key == targetKey) {
                            if (depth == path.size - 1) {
                                return item.children
                            } else if (item.children != null) {
                                return findChildren(item.children, path, depth + 1)
                            }
                        }
                    }
                    return null
                }
                findChildren(uiState.scripts, pathStack, 0) ?: emptyList()
            }
        }

        // 当前路径显示
        val currentPath = pathStack.joinToString(" / ")

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                QingLongTopBar(
                    title = "脚本管理",
                    onMenuClick = onMenuClick,
                    actions = {
                        IconButton(onClick = { viewModel.startCreating() }) {
                            Icon(Icons.Default.Add, contentDescription = "新建")
                        }
                        IconButton(onClick = { viewModel.loadScripts() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                )

                // 路径导航栏
                if (pathStack.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (pathStack.isNotEmpty()) {
                                        pathStack = pathStack.dropLast(1)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = currentPath.ifBlank { "/" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 内容区域
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        uiState.isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        uiState.error != null -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = uiState.error ?: "未知错误", color = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { viewModel.loadScripts() }) { Text("重试") }
                            }
                        }
                        currentItems.isEmpty() -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("暂无文件", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(currentItems, key = { it.key }) { item ->
                                    ScriptFileItem(
                                        item = item,
                                        onClick = {
                                            if (item.type == "directory") {
                                                pathStack = pathStack + item.key
                                            } else if (item.type == "file") {
                                                editingKey = item.key
                                                editingTitle = item.title
                                                editingContent = ""
                                                showEditor = true
                                            }
                                        },
                                        onLongClick = {
                                            viewModel.showScriptActionDialog(item)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        }

        // ===================== 新建脚本/目录弹窗 =====================
        if (uiState.showCreateDialog) {
            var newFileName by remember { mutableStateOf("") }
            var newContent by remember { mutableStateOf("") }
            var isDirectory by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { viewModel.cancelCreating() },
                title = { Text(if (isDirectory) "新建目录" else "新建脚本") },
                text = {
                    Column {
                        // 脚本/目录切换
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !isDirectory,
                                onClick = { isDirectory = false },
                                label = { Text("脚本") }
                            )
                            FilterChip(
                                selected = isDirectory,
                                onClick = { isDirectory = true },
                                label = { Text("目录") }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newFileName,
                            onValueChange = { newFileName = it },
                            label = { Text(if (isDirectory) "目录名" else "文件名") },
                            placeholder = { Text(if (isDirectory) "例如: my_folder" else "例如: test.js") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!isDirectory) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newContent,
                                onValueChange = { newContent = it },
                                label = { Text("内容") },
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val fullPath = if (pathStack.isNotEmpty()) {
                                pathStack.joinToString("/") + "/" + newFileName
                            } else {
                                newFileName
                            }
                            if (isDirectory) {
                                viewModel.createDirectory(fullPath)
                            } else {
                                viewModel.createScript(fullPath, newContent)
                            }
                        }
                    ) { Text("创建") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelCreating() }) { Text("取消") }
                }
            )
        }
    }

    // ===================== 脚本操作弹窗 =====================
    if (uiState.showScriptActionDialog && uiState.actionScript != null) {
        val script = uiState.actionScript!!
        AlertDialog(
            onDismissRequest = { viewModel.hideScriptActionDialog() },
            title = { Text(script.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    if (script.type == "file") {
                        TextButton(
                            onClick = {
                                viewModel.hideScriptActionDialog()
                                editingKey = script.key
                                editingTitle = script.title
                                editingContent = ""
                                showEditor = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("编辑")
                        }
                    }
                    TextButton(
                        onClick = {
                            viewModel.hideScriptActionDialog()
                            viewModel.showRenameDialog(script)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("重命名")
                    }
                    if (script.type == "file") {
                        TextButton(
                            onClick = {
                                viewModel.hideScriptActionDialog()
                                viewModel.runScript(script)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("运行")
                        }
                    }
                    TextButton(
                        onClick = {
                            viewModel.hideScriptActionDialog()
                            viewModel.requestDeleteScript(script)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("删除")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.hideScriptActionDialog() }) { Text("取消") }
            }
        )
    }

    // ===================== 删除确认弹窗 =====================
    if (uiState.showDeleteConfirm && uiState.deletingScript != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${uiState.deletingScript!!.title}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) { Text("取消") }
            }
        )
    }

    // ===================== 重命名弹窗 =====================
    if (uiState.showRenameDialog && uiState.renamingScript != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideRenameDialog() },
            title = { Text("重命名") },
            text = {
                OutlinedTextField(
                    value = uiState.renameText,
                    onValueChange = { viewModel.updateRenameText(it) },
                    label = { Text("新名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRename() }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideRenameDialog() }) { Text("取消") }
            }
        )
    }

    // ===================== 日志弹窗 =====================
    if (uiState.showLogDialog && uiState.logScript != null) {
        val script = uiState.logScript!!
        AlertDialog(
            onDismissRequest = { viewModel.hideLogDialog() },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "运行日志: ${script.title}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    when {
                        uiState.isLoadingLog -> {
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
                        uiState.logContent.isBlank() -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("暂无日志", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        else -> {
                            val scrollState = rememberScrollState()
                            LaunchedEffect(uiState.logContent) {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                            Text(
                                text = uiState.logContent,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.hideLogDialog() }) { Text("关闭") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScriptFileItem(
    item: ScriptItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isDirectory = item.type == "directory"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isDirectory) Icons.Default.Folder else getFileIcon(item.title),
                contentDescription = null,
                tint = if (isDirectory) cOrange else cBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.size != null && !isDirectory) {
                    Text(
                        text = formatFileSize(item.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            if (isDirectory) {
                @Suppress("DEPRECATION")
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "进入",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private fun getFileIcon(filename: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        filename.endsWith(".js") -> Icons.Default.Code
        filename.endsWith(".py") -> Icons.Default.Code
        filename.endsWith(".sh") -> Icons.Default.Terminal
        filename.endsWith(".json") -> Icons.Default.DataObject
        filename.endsWith(".xml") -> Icons.Default.DataObject
        filename.endsWith(".yml") || filename.endsWith(".yaml") -> Icons.Default.Settings
        filename.endsWith(".md") -> Icons.AutoMirrored.Filled.Article
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
    }
}

private fun getLanguageFromExtension(filename: String): String {
    return when {
        filename.endsWith(".js") -> "javascript"
        filename.endsWith(".py") -> "python"
        filename.endsWith(".sh") -> "shell"
        filename.endsWith(".json") -> "json"
        filename.endsWith(".xml") -> "xml"
        filename.endsWith(".yml") || filename.endsWith(".yaml") -> "yaml"
        filename.endsWith(".md") -> "markdown"
        filename.endsWith(".java") -> "java"
        filename.endsWith(".kt") || filename.endsWith(".kts") -> "kotlin"
        filename.endsWith(".ts") -> "typescript"
        filename.endsWith(".css") -> "css"
        filename.endsWith(".html") || filename.endsWith(".htm") -> "html"
        filename.endsWith(".sql") -> "sql"
        filename.endsWith(".go") -> "go"
        filename.endsWith(".rs") -> "rust"
        filename.endsWith(".rb") -> "ruby"
        filename.endsWith(".php") -> "php"
        filename.endsWith(".swift") -> "swift"
        filename.endsWith(".c") || filename.endsWith(".cpp") || filename.endsWith(".h") -> "cpp"
        else -> "plaintext"
    }
}
