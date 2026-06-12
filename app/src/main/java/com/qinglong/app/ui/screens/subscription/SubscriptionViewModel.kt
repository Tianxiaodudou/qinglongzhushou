package com.qinglong.app.ui.screens.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.model.CronLogFile
import com.qinglong.app.data.model.Subscription
import com.qinglong.app.data.repository.Result
import com.qinglong.app.data.repository.TaskRepository
import com.qinglong.app.util.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val subscriptions: List<Subscription> = emptyList(),
    val searchQuery: String = "",
    val showSearch: Boolean = false,
    // 批量操作
    val isBatchMode: Boolean = false,
    val selectedIds: Set<Int> = emptySet(),
    // 创建/编辑弹窗
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingSubscription: Subscription? = null,
    // 删除确认
    val showDeleteConfirm: Boolean = false,
    val deletingSubscription: Subscription? = null,
    val deleteForce: Boolean = false,  // 是否同时删除关联任务和脚本
    // 日志弹窗
    val showLogDialog: Boolean = false,
    val logSubscription: Subscription? = null,
    val logContent: String = "",
    val isLoadingLog: Boolean = false,
    val autoRefreshEnabled: Boolean = true,
    val logFiles: List<CronLogFile> = emptyList(),
    val isLoadingLogFiles: Boolean = false,
    val isLogBatchMode: Boolean = false,
    val selectedLogFiles: Set<String> = emptySet(),
    // 历史日志详情
    val isShowingLogDetail: Boolean = false,
    val selectedLogFile: CronLogFile? = null,
    val showDeleteLogConfirm: Boolean = false,
    val deletingLogFile: CronLogFile? = null,
    val pageSize: Int = 10
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val appSettings: AppSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // 实时日志轮询任务
    private var logPollingJob: Job? = null

    init {
        viewModelScope.launch {
            appSettings.subPageSize.collect { size ->
                _uiState.value = _uiState.value.copy(pageSize = size)
            }
        }
        // 只在首次加载时拉数据，后续页面切换复用缓存
        if (_uiState.value.subscriptions.isEmpty()) {
            loadSubscriptions()
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun loadSubscriptions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = taskRepository.getSubscriptions()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, subscriptions = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            when (val result = taskRepository.getSubscriptions()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isRefreshing = false, subscriptions = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isRefreshing = false, error = result.message) }
                }
            }
        }
    }

    fun toggleSearch() {
        _uiState.update { it.copy(showSearch = !it.showSearch, searchQuery = "") }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleBatchMode() {
        _uiState.update { it.copy(isBatchMode = !it.isBatchMode, selectedIds = emptySet()) }
    }

    fun toggleSelection(id: Int) {
        _uiState.update { state ->
            val newSelected = if (id in state.selectedIds) {
                state.selectedIds - id
            } else {
                state.selectedIds + id
            }
            state.copy(selectedIds = newSelected)
        }
    }

    fun selectAll() {
        _uiState.update { it.copy(selectedIds = it.subscriptions.map { s -> s.id }.toSet()) }
    }

    fun invertSelection() {
        _uiState.update {
            val allIds = it.subscriptions.map { s -> s.id }.toSet()
            it.copy(selectedIds = allIds - it.selectedIds)
        }
    }

    fun deselectAll() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun showEditDialog(subscription: Subscription) {
        _uiState.update { it.copy(showEditDialog = true, editingSubscription = subscription) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, editingSubscription = null) }
    }

    fun createSubscription(body: Map<String, Any>) {
        viewModelScope.launch {
            when (val result = taskRepository.createSubscription(body)) {
                is Result.Success -> {
                    _toastMessage.value = "订阅创建成功"
                    hideCreateDialog()
                    loadSubscriptions()
                }
                is Result.Error -> {
                    _toastMessage.value = "创建失败: ${result.message}"
                }
            }
        }
    }

    fun updateSubscription(id: Int, body: Map<String, Any>) {
        viewModelScope.launch {
            val fullBody = body.toMutableMap()
            fullBody["id"] = id
            when (val result = taskRepository.updateSubscription(fullBody)) {
                is Result.Success -> {
                    _toastMessage.value = "订阅更新成功"
                    hideEditDialog()
                    loadSubscriptions()
                }
                is Result.Error -> {
                    _toastMessage.value = "更新失败: ${result.message}"
                }
            }
        }
    }

    fun showDeleteConfirm(subscription: Subscription) {
        _uiState.update { it.copy(showDeleteConfirm = true, deletingSubscription = subscription, deleteForce = false) }
    }

    fun setDeleteForce(force: Boolean) {
        _uiState.update { it.copy(deleteForce = force) }
    }

    fun hideDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = false, deletingSubscription = null, deleteForce = false) }
    }

    fun deleteSubscription(id: Int) {
        val force = _uiState.value.deleteForce
        viewModelScope.launch {
            when (val result = taskRepository.deleteSubscription(id, force)) {
                is Result.Success -> {
                    _toastMessage.value = "订阅已删除"
                    hideDeleteConfirm()
                    loadSubscriptions()
                }
                is Result.Error -> {
                    _toastMessage.value = "删除失败: ${result.message}"
                }
            }
        }
    }

    fun runSubscription(id: Int) {
        // 乐观更新：立即设置卡片为"运行中"，不等 API 返回
        _uiState.update { state ->
            state.copy(
                subscriptions = state.subscriptions.map { sub ->
                    if (sub.id == id) sub.copy(status = 0, pid = 1) else sub
                }
            )
        }
        // 后台发起 API 调用（静默失败，不影响 UI）
        viewModelScope.launch {
            taskRepository.runSubscription(id)
        }
    }

    fun stopSubscription(id: Int) {
        // 乐观更新：立即设置为"空闲"
        _uiState.update { state ->
            state.copy(
                subscriptions = state.subscriptions.map { sub ->
                    if (sub.id == id) sub.copy(status = 1, pid = null) else sub
                }
            )
        }
        viewModelScope.launch {
            taskRepository.batchStopSubscriptions(listOf(id))
        }
    }

    fun enableSubscription(id: Int) {
        viewModelScope.launch {
            when (val result = taskRepository.batchEnableSubscriptions(listOf(id))) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            subscriptions = state.subscriptions.map { sub ->
                                if (sub.id == id) sub.copy(isDisabled = 0) else sub
                            }
                        )
                    }
                }
                is Result.Error -> {
                    _toastMessage.value = "启用失败: ${result.message}"
                }
            }
        }
    }

    fun disableSubscription(id: Int) {
        viewModelScope.launch {
            when (val result = taskRepository.batchDisableSubscriptions(listOf(id))) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            subscriptions = state.subscriptions.map { sub ->
                                if (sub.id == id) sub.copy(isDisabled = 1) else sub
                            }
                        )
                    }
                }
                is Result.Error -> {
                    _toastMessage.value = "禁用失败: ${result.message}"
                }
            }
        }
    }

    // ===================== 批量操作 =====================

    fun batchRun() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskRepository.batchRunSubscriptions(ids)
            _uiState.update { it.copy(selectedIds = emptySet(), isBatchMode = false) }
            loadSubscriptions()
        }
    }

    fun batchStop() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskRepository.batchStopSubscriptions(ids)
            _uiState.update { it.copy(selectedIds = emptySet(), isBatchMode = false) }
            loadSubscriptions()
        }
    }

    fun batchEnable() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskRepository.batchEnableSubscriptions(ids)
            _uiState.update { it.copy(selectedIds = emptySet(), isBatchMode = false) }
            loadSubscriptions()
        }
    }

    fun batchDisable() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskRepository.batchDisableSubscriptions(ids)
            _uiState.update { it.copy(selectedIds = emptySet(), isBatchMode = false) }
            loadSubscriptions()
        }
    }

    fun batchDelete() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            for (id in ids) {
                taskRepository.deleteSubscription(id, force = true)
            }
            _uiState.update { it.copy(selectedIds = emptySet(), isBatchMode = false) }
            loadSubscriptions()
        }
    }

    // ===================== 日志弹窗 =====================

    fun showLogDialog(subscription: Subscription) {
        // 取消之前的轮询
        logPollingJob?.cancel()

        _uiState.update { it.copy(showLogDialog = true, logSubscription = subscription, autoRefreshEnabled = true) }
        if (subscription.isRunning) {
            // 运行中：加载最新日志并启动自动轮询
            loadLatestLog(subscription.id)
            startLogPolling(subscription.id)
        } else {
            // 非运行中：加载历史日志列表
            loadLogFiles(subscription)
        }
    }

    fun hideLogDialog() {
        // 取消实时日志轮询
        logPollingJob?.cancel()
        logPollingJob = null
        _uiState.update { it.copy(
            showLogDialog = false, logSubscription = null, logContent = "",
            logFiles = emptyList(), isLoadingLogFiles = false,
            isLogBatchMode = false, selectedLogFiles = emptySet(),
            isShowingLogDetail = false, selectedLogFile = null,
            showDeleteLogConfirm = false, deletingLogFile = null
        ) }
    }

    private fun loadLogFiles(subscription: Subscription) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLogFiles = true) }
            // 订阅历史日志列表: GET /api/subscriptions/:id/logs
            when (val result = taskRepository.getSubscriptionLogFiles(subscription.id)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoadingLogFiles = false, logFiles = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingLogFiles = false, logFiles = emptyList()) }
                }
            }
        }
    }

    /**
     * 切换自动刷新
     */
    fun toggleAutoRefresh() {
        val currentSub = _uiState.value.logSubscription ?: return
        val newState = !_uiState.value.autoRefreshEnabled
        _uiState.update { it.copy(autoRefreshEnabled = newState) }
        if (newState) {
            startLogPolling(currentSub.id)
        } else {
            logPollingJob?.cancel()
        }
    }

    /**
     * 手动刷新日志
     */
    fun manualRefreshLog() {
        val currentSub = _uiState.value.logSubscription ?: return
        loadLatestLog(currentSub.id)
    }

    /**
     * 启动实时日志轮询
     */
    private fun startLogPolling(subId: Int) {
        logPollingJob?.cancel()
        logPollingJob = viewModelScope.launch {
            // 首次读取刷新间隔
            var intervalMs = appSettings.subLogRefreshMsState.value.toLong()
            // 监听刷新间隔变化
            launch {
                appSettings.subLogRefreshMsState.collect { ms ->
                    intervalMs = ms.toLong()
                }
            }
            while (true) {
                delay(intervalMs)
                when (val result = taskRepository.getSubscriptionLog(subId)) {
                    is Result.Success -> {
                        _uiState.update { it.copy(logContent = result.data) }
                    }
                    is Result.Error -> {
                        // 静默失败，不阻塞轮询
                    }
                }
            }
        }
    }

    private fun loadLatestLog(subId: Int) {
        _uiState.update { it.copy(isLoadingLog = true, logContent = "") }
        viewModelScope.launch {
            when (val result = taskRepository.getSubscriptionLog(subId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(logContent = result.data, isLoadingLog = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(logContent = "加载失败: ${result.message}", isLoadingLog = false) }
                }
            }
        }
    }

    // ===================== 历史日志操作 =====================

    fun selectLogFile(file: CronLogFile) {
        _uiState.update { it.copy(selectedLogFile = file, isShowingLogDetail = true, isLoadingLog = true, logContent = "") }
        // 加载该历史日志文件的内容
        // 官方: GET /api/logs/detail?file=xxx&path=xxx
        viewModelScope.launch {
            when (val result = taskRepository.getLogDetail(file.filename, file.directory)) {
                is Result.Success -> {
                    _uiState.update { it.copy(logContent = result.data, isLoadingLog = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(logContent = "加载失败: ${result.message}", isLoadingLog = false) }
                }
            }
        }
    }

    fun backToLogFileList() {
        _uiState.update { it.copy(isShowingLogDetail = false, selectedLogFile = null, logContent = "") }
    }

    fun showDeleteLogConfirm(file: CronLogFile) {
        _uiState.update { it.copy(showDeleteLogConfirm = true, deletingLogFile = file) }
    }

    fun hideDeleteLogConfirm() {
        _uiState.update { it.copy(showDeleteLogConfirm = false, deletingLogFile = null) }
    }

    fun confirmDeleteLogFile() {
        val file = _uiState.value.deletingLogFile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(showDeleteLogConfirm = false) }
            when (taskRepository.deleteLogFile(file.directory, file.filename)) {
                is Result.Success<*> -> {
                    _toastMessage.value = "日志已删除"
                    val sub = _uiState.value.logSubscription ?: return@launch
                    // 如果正在查看该日志详情，返回列表
                    if (_uiState.value.isShowingLogDetail) {
                        backToLogFileList()
                    }
                    loadLogFiles(sub)
                }
                is Result.Error -> {
                    _toastMessage.value = "删除失败"
                }
            }
        }
    }

    fun toggleLogBatchMode() {
        _uiState.update { it.copy(isLogBatchMode = !it.isLogBatchMode, selectedLogFiles = emptySet()) }
    }

    fun toggleLogSelection(id: String) {
        _uiState.update { state ->
            val newSelected = if (id in state.selectedLogFiles) {
                state.selectedLogFiles - id
            } else {
                state.selectedLogFiles + id
            }
            state.copy(selectedLogFiles = newSelected)
        }
    }

    fun selectAllLogFiles() {
        _uiState.update { it.copy(selectedLogFiles = it.logFiles.map { f -> f.filename }.toSet()) }
    }

    fun deselectAllLogFiles() {
        _uiState.update { it.copy(selectedLogFiles = emptySet()) }
    }

    fun batchDeleteLogFiles() {
        val ids = _uiState.value.selectedLogFiles.toList()
        if (ids.isEmpty()) return
        val sub = _uiState.value.logSubscription ?: return
        viewModelScope.launch {
            var successCount = 0
            var failCount = 0
            for (filename in ids) {
                val logFile = _uiState.value.logFiles.find { it.filename == filename }
                if (logFile != null) {
                    when (taskRepository.deleteLogFile(logFile.directory, logFile.filename)) {
                        is Result.Success<*> -> successCount++
                        is Result.Error -> failCount++
                    }
                }
            }
            _toastMessage.value = "已删除 $successCount 个日志${if (failCount > 0) "，$failCount 个失败" else ""}"
            toggleLogBatchMode()
            loadLogFiles(sub)
        }
    }

    fun requestDeleteLogFile(logFile: CronLogFile) {
        viewModelScope.launch {
            when (taskRepository.deleteLogFile(logFile.directory, logFile.filename)) {
                is Result.Success<*> -> {
                    _toastMessage.value = "日志已删除"
                    val sub = _uiState.value.logSubscription ?: return@launch
                    loadLogFiles(sub)
                }
                is Result.Error -> {
                    _toastMessage.value = "删除失败"
                }
            }
        }
    }
}

// ===================== Subscription 扩展 =====================

/** 将 Subscription 转换为类似 Task 的结构，以便复用日志弹窗组件 */
fun Subscription.toTaskLike(): com.qinglong.app.data.model.Task {
    return com.qinglong.app.data.model.Task(
        id = this.id,
        name = this.name,
        command = "",
        schedule = this.schedule ?: "",
        status = if (this.isRunning) 0 else 1,
        pid = this.pid,
        isDisabled = this.isDisabled,
        isSystem = 0,
        isPinned = 0,
        labels = null,
        last_running_time = null,
        last_execution_time = null,
        sub_id = null,
        log_path = this.log_path,
        log_name = null,
        extra_schedules = null,
        task_before = null,
        task_after = null,
        allow_multiple_instances = null,
        createdAt = null,
        updatedAt = null
    )
}

/** 订阅是否正在运行（status=0 且有 pid） */
val Subscription.isRunning: Boolean
    get() = this.status == 0 && this.pid != null && this.pid > 0
