package com.qinglong.app.ui.screens.script

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.api.QingLongWebSocketManager
import com.qinglong.app.data.api.SockMessage
import com.qinglong.app.data.model.ScriptItem
import com.qinglong.app.data.repository.AuthRepository
import com.qinglong.app.data.repository.Result
import com.qinglong.app.data.repository.TaskRepository
import com.qinglong.app.util.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

data class ScriptUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val scripts: List<ScriptItem> = emptyList(),
    val searchQuery: String = "",
    val showSearch: Boolean = false,
    // 当前选中的脚本
    val selectedScript: ScriptItem? = null,
    val scriptContent: String = "",
    val isLoadingContent: Boolean = false,
    // 创建/编辑弹窗
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingScript: ScriptItem? = null,
    // 删除确认
    val showDeleteConfirm: Boolean = false,
    val deletingScript: ScriptItem? = null,
    // 脚本操作弹窗
    val showScriptActionDialog: Boolean = false,
    val actionScript: ScriptItem? = null,
    // 重命名弹窗
    val showRenameDialog: Boolean = false,
    val renamingScript: ScriptItem? = null,
    val renameText: String = "",
    // 日志弹窗
    val showLogDialog: Boolean = false,
    val logScript: ScriptItem? = null,
    val logContent: String = "",
    val isLoadingLog: Boolean = false,
    // 成功消息
    val successMessage: String? = null
)

@HiltViewModel
class ScriptViewModel @Inject constructor(
    val taskRepository: TaskRepository,
    val authRepository: AuthRepository,
    val appSettings: AppSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScriptUiState())
    val uiState: StateFlow<ScriptUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // WebSocket 管理器
    private var wsManager: QingLongWebSocketManager? = null

    init {
        loadScripts()
    }

    override fun onCleared() {
        super.onCleared()
        wsManager?.disconnect()
    }

    fun loadScripts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = taskRepository.getScripts()) {
                is Result.Success -> {
                    // 不对文件做任何过滤，和网页端树状文件管理保持一致
                    _uiState.update { it.copy(isLoading = false, scripts = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun loadScriptContent(key: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingContent = true, scriptContent = "") }
            when (val result = taskRepository.getScriptDetail(key)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingContent = false,
                            scriptContent = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingContent = false) }
                    _toastMessage.value = "加载脚本内容失败 (错误码: ${result.code}): ${result.message}"
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearch() {
        _uiState.update { it.copy(showSearch = !it.showSearch, searchQuery = "") }
    }

    fun selectScript(script: ScriptItem) {
        _uiState.update { it.copy(selectedScript = script) }
        loadScriptContent(script.key)
    }

    fun deleteScript(key: String) {
        viewModelScope.launch {
            // 从 key 中拆分 filename 和 path
            val lastSlash = key.lastIndexOf('/')
            val filename = if (lastSlash >= 0) key.substring(lastSlash + 1) else key
            val path = if (lastSlash >= 0) key.substring(0, lastSlash) else ""
            when (val result = taskRepository.deleteScripts(filename, path)) {
                is Result.Success -> {
                    _toastMessage.value = "脚本已删除"
                    cancelDelete()
                    if (_uiState.value.selectedScript?.key == key) {
                        _uiState.update { it.copy(selectedScript = null, scriptContent = "") }
                    }
                    loadScripts()
                }
                is Result.Error -> {
                    _toastMessage.value = "删除失败 (错误码: ${result.code}): ${result.message}"
                }
            }
        }
    }

    fun startCreating() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun cancelCreating() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun startEditing(script: ScriptItem) {
        _uiState.update { it.copy(showEditDialog = true, editingScript = script) }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(showEditDialog = false, editingScript = null) }
    }

    fun createScript(path: String, content: String) {
        viewModelScope.launch {
            // 从 path 中拆分 filename 和 dir
            val lastSlash = path.lastIndexOf('/')
            val filename = if (lastSlash >= 0) path.substring(lastSlash + 1) else path
            val dir = if (lastSlash >= 0) path.substring(0, lastSlash) else ""
            val body = mapOf<String, Any>(
                "filename" to filename,
                "path" to dir,
                "content" to content
            )
            when (val result = taskRepository.createScript(body)) {
                is Result.Success -> {
                    _toastMessage.value = "脚本创建成功"
                    cancelCreating()
                    loadScripts()
                }
                is Result.Error -> {
                    _toastMessage.value = "创建失败 (错误码: ${result.code}): ${result.message}"
                }
            }
        }
    }

    fun createDirectory(path: String) {
        viewModelScope.launch {
            // 从 path 中拆分 directory 和 dir
            val lastSlash = path.lastIndexOf('/')
            val directory = if (lastSlash >= 0) path.substring(lastSlash + 1) else path
            val dir = if (lastSlash >= 0) path.substring(0, lastSlash) else ""
            val body = mapOf<String, Any>(
                "filename" to directory,
                "path" to dir,
                "content" to "",
                "directory" to directory
            )
            when (val result = taskRepository.createScript(body)) {
                is Result.Success -> {
                    _toastMessage.value = "目录创建成功"
                    cancelCreating()
                    loadScripts()
                }
                is Result.Error -> {
                    _toastMessage.value = "创建失败 (错误码: ${result.code}): ${result.message}"
                }
            }
        }
    }

    fun saveScript(key: String, content: String) {
        viewModelScope.launch {
            // 从 key 中拆分 filename 和 path
            val lastSlash = key.lastIndexOf('/')
            val filename = if (lastSlash >= 0) key.substring(lastSlash + 1) else key
            val path = if (lastSlash >= 0) key.substring(0, lastSlash) else ""
            val body = mapOf<String, Any>(
                "filename" to filename,
                "path" to path,
                "content" to content
            )
            when (val result = taskRepository.updateScript(body)) {
                is Result.Success -> {
                    _toastMessage.value = "脚本保存成功"
                    loadScripts()
                }
                is Result.Error -> {
                    _toastMessage.value = "保存失败 (错误码: ${result.code}): ${result.message}"
                }
            }
        }
    }

    /**
     * 从编辑器运行脚本：将当前内容发给服务器运行（不保存），然后通过 WebSocket 获取实时日志
     *
     * 青龙面板运行脚本后，日志通过 SockJS WebSocket 实时推送。
     * 网页端前端订阅 manuallyRunScript 事件接收实时日志。
     * APP 端使用 OkHttp WebSocket 按照 SockJS 协议连接，同样订阅 manuallyRunScript 事件。
     */
    fun runScriptAndGetLog(key: String, content: String) {
        viewModelScope.launch {
            // 设置加载状态
            _uiState.update { it.copy(isLoadingLog = true, logContent = "") }

            // 从 key 中拆分 filename 和 path
            val lastSlash = key.lastIndexOf('/')
            val filename = if (lastSlash >= 0) key.substring(lastSlash + 1) else key
            val path = if (lastSlash >= 0) key.substring(0, lastSlash) else ""

            // 先建立 WebSocket 连接
            val server = authRepository.loadServerConfig()
            val token = authRepository.getToken()
            if (server == null || token == null) {
                _uiState.update { it.copy(logContent = "无法连接：服务器配置或 Token 缺失", isLoadingLog = false) }
                return@launch
            }

            val baseUrl = "${server.protocol}://${server.domain}:${server.port}"
            val wsManager = QingLongWebSocketManager(baseUrl, token)
            this@ScriptViewModel.wsManager = wsManager

            // 订阅 manuallyRunScript 事件
            val logContentBuilder = StringBuilder()
            wsManager.subscribe("manuallyRunScript") { message ->
                logContentBuilder.append(message.message)
                logContentBuilder.append("\n")
                _uiState.update { it.copy(logContent = logContentBuilder.toString()) }
            }

            // 连接 WebSocket（设置5秒超时）
            val connectResult = try {
                withTimeout(5000) {
                    val ok = wsManager.connect()
                    if (ok) "成功" else "连接失败（服务器返回错误）"
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                "连接超时（5秒）"
            } catch (e: Exception) {
                "连接异常: ${e.message ?: e.javaClass.simpleName}"
            }

            if (connectResult != "成功") {
                // WebSocket 连接失败，直接显示错误信息（不再回退到轮询，因为轮询也拿不到日志）
                _uiState.update {
                    it.copy(
                        logContent = "无法获取实时日志\n\n" +
                                "WebSocket 连接结果: $connectResult\n\n" +
                                "青龙面板运行脚本的日志只通过 WebSocket 实时推送，\n" +
                                "不会写入文件系统。请检查：\n" +
                                "1. 青龙面板版本是否支持 WebSocket（≥ 2.10）\n" +
                                "2. 网络连接是否正常\n" +
                                "3. 服务器地址和端口是否正确\n" +
                                "4. Token 是否有效",
                        isLoadingLog = false
                    )
                }
                return@launch
            }

            // 运行脚本（不保存，直接把内容发给服务器运行）
            when (val result = taskRepository.runScript(filename, path, content)) {
                is Result.Success -> {
                    val intervalMs = appSettings.scriptRunRefreshMsState.value.toLong().coerceAtMost(1000L)
                    // 等待日志（最多 60 秒）
                    var waitCount = 0
                    val maxWait = (60000 / intervalMs).toInt()
                    while (waitCount < maxWait) {
                        delay(intervalMs)
                        waitCount++
                        // 如果已经收到日志内容，继续等待直到结束
                        if (logContentBuilder.isNotEmpty()) {
                            // 检查是否结束（日志末尾包含结束标记）
                            val currentLog = logContentBuilder.toString()
                            if (currentLog.contains("结束") || currentLog.contains("完成") || currentLog.contains("exit")) {
                                delay(500) // 等最后的消息
                                break
                            }
                        }
                    }
                    _uiState.update { it.copy(isLoadingLog = false) }
                    if (logContentBuilder.isEmpty()) {
                        _uiState.update { it.copy(logContent = "脚本已运行，但未收到实时日志", isLoadingLog = false) }
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(logContent = "运行失败: ${result.message}", isLoadingLog = false) }
                }
            }

            // 断开 WebSocket
            wsManager.disconnect()
        }
    }

    /**
     * 回退方案：轮询 GET /api/logs/detail 获取日志
     */
    private suspend fun runScriptAndPollLog(filename: String, path: String, content: String) {
        // 运行脚本
        when (val result = taskRepository.runScript(filename, path, content)) {
            is Result.Success -> {
                val (name, _) = parseFilename(filename)
                val logFilename = "${name}.swap"
                val intervalMs = appSettings.scriptRunRefreshMsState.value.toLong().coerceAtMost(1000L)
                val maxRetries = (60000 / intervalMs).toInt()
                var retryCount = 0

                while (retryCount < maxRetries) {
                    delay(intervalMs)
                    retryCount++

                    when (val logResult = taskRepository.getLogDetail(logFilename, path)) {
                        is Result.Success -> {
                            val logData = logResult.data
                            if (logData.isNotEmpty()) {
                                _uiState.update { it.copy(logContent = logData, isLoadingLog = false) }
                                return
                            }
                        }
                        is Result.Error -> { }
                    }
                }
                _uiState.update { it.copy(logContent = "日志获取超时（60秒）", isLoadingLog = false) }
            }
            is Result.Error -> {
                _uiState.update { it.copy(logContent = "运行失败: ${result.message}", isLoadingLog = false) }
            }
        }
    }

    /**
     * 解析文件名，返回 name 和 ext
     */
    private fun parseFilename(filename: String): Pair<String, String> {
        val dotIndex = filename.lastIndexOf('.')
        return if (dotIndex >= 0) {
            Pair(filename.substring(0, dotIndex), filename.substring(dotIndex))
        } else {
            Pair(filename, "")
        }
    }

    fun updateCreateFilename(@Suppress("UNUSED_PARAMETER") filename: String) {
        // 更新创建对话框中的文件名
    }

    fun updateCreateContent(@Suppress("UNUSED_PARAMETER") content: String) {
        // 更新创建对话框中的内容
    }

    fun updateEditContent(content: String) {
        _uiState.update { it.copy(scriptContent = content) }
    }

    fun requestDeleteScript(script: ScriptItem) {
        _uiState.update { it.copy(showDeleteConfirm = true, deletingScript = script) }
    }

    fun confirmDelete() {
        val script = _uiState.value.deletingScript ?: return
        viewModelScope.launch {
            val lastSlash = script.key.lastIndexOf('/')
            val filename = if (lastSlash >= 0) script.key.substring(lastSlash + 1) else script.key
            val path = if (lastSlash >= 0) script.key.substring(0, lastSlash) else ""
            when (val result = taskRepository.deleteScripts(filename, path)) {
                is Result.Success -> {
                    _toastMessage.value = "脚本已删除"
                    cancelDelete()
                    if (_uiState.value.selectedScript?.key == script.key) {
                        _uiState.update { it.copy(selectedScript = null, scriptContent = "") }
                    }
                    loadScripts()
                }
                is Result.Error -> {
                    _toastMessage.value = "删除失败: ${result.message}"
                }
            }
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteConfirm = false, deletingScript = null) }
    }

    fun showScriptActionDialog(script: ScriptItem) {
        _uiState.update { it.copy(showScriptActionDialog = true, actionScript = script) }
    }

    fun hideScriptActionDialog() {
        _uiState.update { it.copy(showScriptActionDialog = false, actionScript = null) }
    }

    fun closeEditor() {
        _uiState.update { it.copy(selectedScript = null, scriptContent = "") }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    // ===================== 重命名 =====================

    fun showRenameDialog(script: ScriptItem) {
        _uiState.update { it.copy(showRenameDialog = true, renamingScript = script, renameText = script.title) }
    }

    fun hideRenameDialog() {
        _uiState.update { it.copy(showRenameDialog = false, renamingScript = null, renameText = "") }
    }

    fun updateRenameText(text: String) {
        _uiState.update { it.copy(renameText = text) }
    }

    fun confirmRename() {
        val script = _uiState.value.renamingScript ?: return
        val newName = _uiState.value.renameText.trim()
        if (newName.isBlank()) {
            _toastMessage.value = "名称不能为空"
            return
        }
        viewModelScope.launch {
            val lastSlash = script.key.lastIndexOf('/')
            val filename = if (lastSlash >= 0) script.key.substring(lastSlash + 1) else script.key
            val path = if (lastSlash >= 0) script.key.substring(0, lastSlash) else ""
            when (val result = taskRepository.renameScript(filename, path, newName)) {
                is Result.Success -> {
                    _toastMessage.value = "重命名成功"
                    hideRenameDialog()
                    loadScripts()
                }
                is Result.Error -> {
                    _toastMessage.value = "重命名失败: ${result.message}"
                }
            }
        }
    }

    fun runScript(script: ScriptItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(showLogDialog = true, logScript = script, logContent = "", isLoadingLog = true) }
            val lastSlash = script.key.lastIndexOf('/')
            val filename = if (lastSlash >= 0) script.key.substring(lastSlash + 1) else script.key
            val path = if (lastSlash >= 0) script.key.substring(0, lastSlash) else ""

            // 先建立 WebSocket 连接
            val server = authRepository.loadServerConfig()
            val token = authRepository.getToken()
            if (server != null && token != null) {
                val baseUrl = "${server.protocol}://${server.domain}:${server.port}"
                val wsManager = QingLongWebSocketManager(baseUrl, token)
                this@ScriptViewModel.wsManager = wsManager

                val logContentBuilder = StringBuilder()
                wsManager.subscribe("manuallyRunScript") { message ->
                    logContentBuilder.append(message.message)
                    logContentBuilder.append("\n")
                    _uiState.update { it.copy(logContent = logContentBuilder.toString()) }
                }

                val connected = wsManager.connect()
                if (connected) {
                    when (val result = taskRepository.runScript(filename, path, "")) {
                        is Result.Success -> {
                            val intervalMs = appSettings.scriptRunRefreshMsState.value.toLong().coerceAtMost(1000L)
                            var waitCount = 0
                            val maxWait = (60000 / intervalMs).toInt()
                            while (waitCount < maxWait) {
                                delay(intervalMs)
                                waitCount++
                                if (logContentBuilder.isNotEmpty()) {
                                    val currentLog = logContentBuilder.toString()
                                    if (currentLog.contains("结束") || currentLog.contains("完成") || currentLog.contains("exit")) {
                                        delay(500)
                                        break
                                    }
                                }
                            }
                            _uiState.update { it.copy(isLoadingLog = false) }
                            if (logContentBuilder.isEmpty()) {
                                _uiState.update { it.copy(logContent = "脚本已运行，但未收到实时日志", isLoadingLog = false) }
                            }
                        }
                        is Result.Error -> {
                            _uiState.update { it.copy(logContent = "运行失败: ${result.message}", isLoadingLog = false) }
                        }
                    }
                    wsManager.disconnect()
                    return@launch
                }
            }

            // WebSocket 连接失败，回退到轮询
            when (val result = taskRepository.runScript(filename, path, "")) {
                is Result.Success -> {
                    _toastMessage.value = "脚本已运行"
                    loadScriptLog(script)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingLog = false) }
                    _toastMessage.value = "运行失败: ${result.message}"
                }
            }
        }
    }

    private suspend fun loadScriptLog(script: ScriptItem) {
        val lastSlash = script.key.lastIndexOf('/')
        val name = if (lastSlash >= 0) script.key.substring(lastSlash + 1) else script.key
        val path = if (lastSlash >= 0) script.key.substring(0, lastSlash) else ""
        when (val result = taskRepository.getLogDetail(name, path)) {
            is Result.Success -> {
                _uiState.update { it.copy(logContent = result.data, isLoadingLog = false) }
            }
            is Result.Error -> {
                _uiState.update { it.copy(logContent = "暂无日志", isLoadingLog = false) }
            }
        }
    }

    fun hideLogDialog() {
        _uiState.update { it.copy(showLogDialog = false, logScript = null, logContent = "", isLoadingLog = false) }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun clearScriptContent() {
        _uiState.update { it.copy(scriptContent = "") }
    }
}
