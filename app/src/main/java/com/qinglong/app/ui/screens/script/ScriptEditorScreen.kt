package com.qinglong.app.ui.screens.script

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qinglong.app.data.api.QingLongWebSocketManager
import com.qinglong.app.data.api.SockMessage
import com.qinglong.app.data.repository.Result
import com.qinglong.app.ui.components.CodeEditor
import com.qinglong.app.ui.components.ErrorDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * 脚本编辑器页面
 * 使用公共 CodeEditor 组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditorScreen(
    scriptKey: String,
    scriptTitle: String,
    language: String,
    viewModel: ScriptViewModel,
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.3f &&
        MaterialTheme.colorScheme.background.green < 0.3f &&
        MaterialTheme.colorScheme.background.blue < 0.3f

    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var initialContent by remember { mutableStateOf("") }
    var hasChanges by remember { mutableStateOf(false) }
    var showBackConfirm by remember { mutableStateOf(false) }
    var showSaveConfirm by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    // 运行日志 - 完全独立管理，不依赖 uiState
    var showLogDialog by remember { mutableStateOf(false) }
    var logDialogContent by remember { mutableStateOf("") }
    var logDialogLoading by remember { mutableStateOf(false) }
    var logDialogError by remember { mutableStateOf<String?>(null) }
    var currentPid by remember { mutableIntStateOf(0) } // 当前运行脚本的 pid
    val coroutineScope = rememberCoroutineScope()

    // 加载脚本内容
    LaunchedEffect(scriptKey) {
        initialContent = ""
        textFieldValue = TextFieldValue("")
        hasChanges = false
        viewModel.loadScriptContent(scriptKey)
    }

    // 内容加载完成后设置到编辑器
    LaunchedEffect(uiState.scriptContent) {
        if (uiState.scriptContent.isNotEmpty() && initialContent.isEmpty()) {
            initialContent = uiState.scriptContent
            textFieldValue = TextFieldValue(uiState.scriptContent)
        }
    }

    // 监听加载失败消息
    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            if (msg.startsWith("加载脚本内容失败")) {
                errorMessage = msg
                showErrorDialog = true
                viewModel.clearToast()
            }
        }
    }

    // 处理返回键
    BackHandler(enabled = true) {
        if (hasChanges) {
            showBackConfirm = true
        } else {
            viewModel.clearScriptContent()
            onBack()
        }
    }

    // 检测内容变化
    LaunchedEffect(textFieldValue.text) {
        hasChanges = textFieldValue.text != initialContent
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = scriptTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges) showBackConfirm = true else {
                            viewModel.clearScriptContent()
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 运行按钮
                    IconButton(
                        onClick = {
                            logDialogContent = ""
                            logDialogError = null
                            logDialogLoading = true
                            showLogDialog = true
                            coroutineScope.launch {
                                val intervalMs = viewModel.appSettings.scriptRunRefreshMsState.value.toLong().coerceAtMost(1000L)
                                // 从 scriptKey 中提取 filename 和 path
                                val lastSlash = scriptKey.lastIndexOf('/')
                                val filename = if (lastSlash >= 0) scriptKey.substring(lastSlash + 1) else scriptKey
                                val path = if (lastSlash >= 0) scriptKey.substring(0, lastSlash) else ""
                                runScriptAndShowLog(
                                    viewModel = viewModel,
                                    scriptKey = scriptKey,
                                    filename = filename,
                                    path = path,
                                    content = textFieldValue.text,
                                    intervalMs = intervalMs,
                                    onLogUpdate = { log ->
                                        logDialogContent = log
                                        logDialogLoading = false
                                    },
                                    onDebugInfo = { info ->
                                        logDialogContent = logDialogContent + "\n" + info
                                    },
                                    onError = { error ->
                                        logDialogError = error
                                        logDialogLoading = false
                                    },
                                    onPid = { pid ->
                                        currentPid = pid
                                    }
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "运行")
                    }

                    // 保存按钮
                    IconButton(
                        onClick = {
                            if (hasChanges) {
                                showSaveConfirm = true
                            } else {
                                viewModel.clearScriptContent()
                                onSave(textFieldValue.text)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            CodeEditor(
                textFieldValue = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                },
                language = language,
                isDarkTheme = isDarkTheme,
                placeholder = "// 在此输入代码...",
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // 返回确认弹窗
    if (showBackConfirm) {
        AlertDialog(
            onDismissRequest = { showBackConfirm = false },
            title = { Text("未保存的更改") },
            text = { Text("您有未保存的更改，确定要退出吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showBackConfirm = false
                    viewModel.clearScriptContent()
                    onBack()
                }) {
                    Text("不保存并退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackConfirm = false }) {
                    Text("继续编辑")
                }
            }
        )
    }

    // 保存确认弹窗
    if (showSaveConfirm) {
        AlertDialog(
            onDismissRequest = { showSaveConfirm = false },
            title = { Text("保存更改") },
            text = { Text("确定要保存更改吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showSaveConfirm = false
                    viewModel.clearScriptContent()
                    onSave(textFieldValue.text)
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 错误弹窗
    if (showErrorDialog) {
        ErrorDialog(
            title = "加载失败",
            message = errorMessage,
            onDismiss = {
                showErrorDialog = false
                viewModel.clearScriptContent()
                onBack()
            }
        )
    }

    // 运行日志弹窗
    if (showLogDialog) {
        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("运行日志", modifier = Modifier.weight(1f))
                    if (currentPid > 0) {
                        TextButton(onClick = {
                            coroutineScope.launch {
                                val lastSlash = scriptKey.lastIndexOf('/')
                                val filename = if (lastSlash >= 0) scriptKey.substring(lastSlash + 1) else scriptKey
                                val path = if (lastSlash >= 0) scriptKey.substring(0, lastSlash) else ""
                                viewModel.taskRepository.stopScript(filename, path, currentPid)
                                showLogDialog = false
                            }
                        }) {
                            Text("停止运行", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            text = {
                Column(modifier = Modifier.heightIn(max = 400.dp)) {
                    if (logDialogLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(8.dp))
                                Text("正在连接服务器...")
                            }
                        }
                    } else if (logDialogError != null) {
                        Text(
                            text = logDialogError ?: "未知错误",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        SelectionContainer {
                            Text(
                                text = logDialogContent.ifEmpty { "无日志输出" },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLogDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

/**
 * 运行脚本并获取日志（独立协程函数，不依赖 uiState）
 */
private suspend fun runScriptAndShowLog(
    viewModel: ScriptViewModel,
    @Suppress("UNUSED_PARAMETER") scriptKey: String,
    filename: String,
    path: String,
    content: String,
    @Suppress("UNUSED_PARAMETER") intervalMs: Long,
    onLogUpdate: (String) -> Unit,
    onDebugInfo: (String) -> Unit,
    onError: (String) -> Unit,
    onPid: (Int) -> Unit
) {
    try {
        // 1. 获取服务器地址和 Token
        val serverConfig = viewModel.authRepository.loadServerConfig()
        val token = viewModel.authRepository.getToken()
        if (serverConfig == null || token.isNullOrBlank()) {
            onError("服务器配置或 Token 为空，请先登录")
            return
        }
        val serverUrl = "${serverConfig.protocol}://${serverConfig.domain}:${serverConfig.port}"

        onDebugInfo("[调试] 服务器: $serverUrl")
        onDebugInfo("[调试] Token: ${token.take(20)}...")

        // 2. 运行脚本
        onDebugInfo("[调试] 正在运行脚本...")
        val runResult = viewModel.taskRepository.runScript(filename, path, content)
        when (runResult) {
            is Result.Success -> {
                val pid = runResult.data
                onPid(pid)
                onDebugInfo("[调试] 脚本已运行，PID: $pid")
            }
            is Result.Error -> {
                onError("运行脚本失败: ${runResult.message}")
                return
            }
        }

        // 3. 建立 WebSocket 连接接收实时日志
        onDebugInfo("[调试] 正在连接 WebSocket...")
        val wsManager = QingLongWebSocketManager(serverUrl, token)
        val logContentBuilder = StringBuilder()
        var hasReceivedLog = false

        // 注册通用消息回调（所有消息）
        wsManager.onAnyMessage { rawMessage ->
            hasReceivedLog = true
            // 尝试从消息中提取日志内容
            try {
                val sockMsg = com.google.gson.Gson().fromJson(rawMessage, com.qinglong.app.data.api.SockMessage::class.java)
                if (sockMsg.message.isNotEmpty()) {
                    logContentBuilder.append(sockMsg.message).append("\n")
                    onLogUpdate(logContentBuilder.toString())
                }
            } catch (_: Exception) {
                logContentBuilder.append(rawMessage).append("\n")
                onLogUpdate(logContentBuilder.toString())
            }
        }

        // 订阅 manuallyRunScript 事件
        wsManager.subscribe("manuallyRunScript") { msg ->
            hasReceivedLog = true
            if (msg.message.isNotEmpty()) {
                logContentBuilder.append(msg.message).append("\n")
                onLogUpdate(logContentBuilder.toString())
            }
        }

        try {
            withTimeout(30_000) {
                val connected = wsManager.connect()
                if (!connected) {
                    onDebugInfo("[WS] 连接失败")
                    return@withTimeout
                }
                onDebugInfo("[WS] 连接成功")

                // 发送订阅消息
                val subscribeMsg = """{"action":"subscribe","topic":"manuallyRunScript"}"""
                wsManager.send(subscribeMsg)
                onDebugInfo("[WS] 已发送订阅消息")

                // 等待日志（最多 30 秒）
                var waitCount = 0
                while (waitCount < 30) {
                    delay(1000)
                    waitCount++
                    if (logContentBuilder.isNotEmpty()) {
                        onLogUpdate(logContentBuilder.toString())
                    }
                }
            }
        } catch (e: Exception) {
            onDebugInfo("[WS] 连接结束: ${e.message}")
        } finally {
            wsManager.disconnect()
        }

        if (!hasReceivedLog && logContentBuilder.isEmpty()) {
            onDebugInfo("[提示] 脚本已成功运行，但未收到实时日志")
            onDebugInfo("[提示] 请检查青龙面板 WebSocket 服务是否正常")
        }
    } catch (e: Exception) {
        onError("运行脚本异常: ${e.message ?: e.javaClass.name}")
    }
}
