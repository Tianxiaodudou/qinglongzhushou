package com.qinglong.app.ui.screens.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.model.CronLogFile
import com.qinglong.app.data.model.CronViewFilter
import com.qinglong.app.data.model.Task
import com.qinglong.app.data.model.ViewItem
import com.qinglong.app.data.repository.Result
import com.qinglong.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,  // 切换标签/搜索时的刷新动画
    val error: String? = null,
    val tasks: List<Task> = emptyList(),
    val searchQuery: String = "",
    val showSearch: Boolean = false,
    val searchMode: Int = 0,  // 0=按名称搜索, 1=按订阅搜索, 2=按标签搜索
    // 视图标签：从 API 动态拉取
    val viewTabs: List<ViewTab> = emptyList(),
    val selectedTabIndex: Int = 0,
    val isBatchMode: Boolean = false,
    val selectedTaskIds: Set<Int> = emptySet(),
    // 弹窗状态
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingTask: Task? = null,
    val showDeleteConfirm: Boolean = false,
    val deletingTask: Task? = null,
    // 日志弹窗
    val showLogDialog: Boolean = false,
    val logTask: Task? = null,
    val logContent: String = "",
    val isLoadingLog: Boolean = false,
    val logFiles: List<CronLogFile> = emptyList(),      // 历史日志文件列表
    val isLoadingLogFiles: Boolean = false,              // 是否正在加载历史日志列表
    val selectedLogFile: CronLogFile? = null,            // 用户选中的历史日志文件
    val isShowingLogDetail: Boolean = false,             // 是否正在查看某条日志详情
    val showDeleteLogConfirm: Boolean = false,            // 是否显示删除日志确认弹窗
    val deletingLogFile: CronLogFile? = null,             // 正在删除的日志文件
    // 历史日志多选
    val isLogBatchMode: Boolean = false,                  // 日志列表是否处于多选模式
    val selectedLogFiles: Set<String> = emptySet(),       // 选中的日志文件标识（用 fullPath）
    // 视图管理弹窗
    val showViewManager: Boolean = false,
    val editingView: ViewTab? = null,
    val showViewDeleteConfirm: Boolean = false,
    val deletingView: ViewTab? = null,
    // 订阅列表（用于视图编辑）
    val subscriptions: List<com.qinglong.app.data.model.Subscription> = emptyList(),
    // 错误提示弹窗
    val showErrorDialog: Boolean = false,
    val errorDialogMessage: String = "",
    // 下次运行时间缓存（key=任务id，value=格式化后的时间字符串）
    val nextRunTimeCache: Map<Int, String> = emptyMap()
)

data class ViewTab(
    val id: Int,
    val name: String,
    val type: Int,           // 1=系统内置, 2=用户自定义
    val filters: List<CronViewFilter>?,
    val filterRelation: String?
)

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // 实时日志轮询任务
    private var logPollingJob: Job? = null

    init {
        loadViews()
        loadSubscriptions()
    }

    private fun loadSubscriptions() {
        viewModelScope.launch {
            when (val result = taskRepository.getSubscriptions()) {
                is Result.Success -> {
                    _uiState.update { it.copy(subscriptions = result.data) }
                }
                is Result.Error -> {
                }
            }
        }
    }

    /**
     * 从 API 加载视图标签，然后加载任务
     */
    fun loadViews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = taskRepository.getTaskViews()) {
                is Result.Success -> {
                    val views = result.data

                    val tabs = if (views.isNotEmpty()) {
                        views.sortedBy { it.position ?: Long.MAX_VALUE }.map { v ->
                            ViewTab(
                                id = v.id,
                                name = v.name,
                                type = v.type,
                                filters = v.filters,
                                filterRelation = v.filterRelation
                            )
                        }
                    } else {
                        // fallback：如果 API 没有返回视图，用默认标签
                        listOf(
                            ViewTab(0, "全部", 1, null, null),
                            ViewTab(0, "运行中", 1,
                                listOf(CronViewFilter("status", "In", "0,0.5")), "and"),
                            ViewTab(0, "已禁用", 1,
                                listOf(CronViewFilter("isDisabled", "In", "1")), "and")
                        )
                    }

                    _uiState.update { it.copy(viewTabs = tabs, isLoading = false) }
                }
                is Result.Error -> {
                    val defaultTabs = listOf(
                        ViewTab(0, "全部", 1, null, null),
                        ViewTab(0, "运行中", 1,
                            listOf(CronViewFilter("status", "In", "0,0.5")), "and"),
                        ViewTab(0, "已禁用", 1,
                            listOf(CronViewFilter("isDisabled", "In", "1")), "and")
                    )
                    _uiState.update { it.copy(viewTabs = defaultTabs, isLoading = false) }
                }
            }

            // 视图加载完成后加载任务
            loadTasks()
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
        loadTasks()
    }

    /**
     * 刷新：重新加载任务列表（保留当前视图标签）
     */
    fun refreshTasks() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val state = _uiState.value
            val query = state.searchQuery
            val currentTab = state.viewTabs.getOrNull(state.selectedTabIndex)

            val (viewFilters, viewFilterRelation) = if (currentTab != null && currentTab.id != 0 && currentTab.filters != null) {
                currentTab.filters to currentTab.filterRelation
            } else if (currentTab != null && currentTab.filters != null) {
                currentTab.filters to (currentTab.filterRelation ?: "and")
            } else {
                null to null
            }

            when (val result = taskRepository.getTasks(
                searchValue = query.takeIf { it.isNotBlank() },
                viewFilters = viewFilters,
                viewFilterRelation = viewFilterRelation
            )) {
                is Result.Success -> {
                    // 一次性计算所有任务的"预计下次运行时间"
                    val cache = mutableMapOf<Int, String>()
                    for (task in result.data) {
                        val nextCal = com.qinglong.app.util.CronParser.getNextRunTime(task.schedule, task.lastRunTime)
                        cache[task.id] = if (nextCal != null) {
                            String.format(
                                "%04d-%02d-%02d %02d:%02d",
                                nextCal.get(java.util.Calendar.YEAR),
                                nextCal.get(java.util.Calendar.MONTH) + 1,
                                nextCal.get(java.util.Calendar.DAY_OF_MONTH),
                                nextCal.get(java.util.Calendar.HOUR_OF_DAY),
                                nextCal.get(java.util.Calendar.MINUTE)
                            )
                        } else {
                            "无法计算"
                        }
                    }
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, tasks = result.data, nextRunTimeCache = cache) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = result.message) }
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) loadTasks()
    }

    fun setSearchMode(mode: Int) {
        _uiState.update { it.copy(searchMode = mode) }
    }

    /**
     * 按任务名称模糊搜索（忽略当前视图标签）
     */
    fun searchByName(query: String) {
        if (query.isBlank()) {
            loadTasks()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = taskRepository.getTasks(
                searchValue = query,
                viewFilters = null,
                viewFilterRelation = null
            )) {
                is Result.Success -> {
                    val cache = mutableMapOf<Int, String>()
                    for (task in result.data) {
                        val nextCal = com.qinglong.app.util.CronParser.getNextRunTime(task.schedule, task.lastRunTime)
                        cache[task.id] = if (nextCal != null) {
                            String.format(
                                "%04d-%02d-%02d %02d:%02d",
                                nextCal.get(java.util.Calendar.YEAR),
                                nextCal.get(java.util.Calendar.MONTH) + 1,
                                nextCal.get(java.util.Calendar.DAY_OF_MONTH),
                                nextCal.get(java.util.Calendar.HOUR_OF_DAY),
                                nextCal.get(java.util.Calendar.MINUTE)
                            )
                        } else {
                            "无法计算"
                        }
                    }
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, tasks = result.data, nextRunTimeCache = cache) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = result.message) }
                }
            }
        }
    }

    /**
     * 按订阅源搜索（客户端过滤）
     * 从全部任务中筛选出订阅名称或ID包含关键词的任务
     */
    fun searchBySubscription(query: String) {
        if (query.isBlank()) {
            loadTasks()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 先获取全部任务
            when (val result = taskRepository.getTasks(
                searchValue = null,
                viewFilters = null,
                viewFilterRelation = null
            )) {
                is Result.Success -> {
                    // 客户端按订阅名称模糊匹配
                    val filtered = result.data.filter { task ->
                        task.sub_id != null && _uiState.value.subscriptions.any { sub ->
                            sub.id == task.sub_id && sub.name.contains(query, ignoreCase = true)
                        }
                    }

                    val cache = mutableMapOf<Int, String>()
                    for (task in filtered) {
                        val nextCal = com.qinglong.app.util.CronParser.getNextRunTime(task.schedule, task.lastRunTime)
                        cache[task.id] = if (nextCal != null) {
                            String.format(
                                "%04d-%02d-%02d %02d:%02d",
                                nextCal.get(java.util.Calendar.YEAR),
                                nextCal.get(java.util.Calendar.MONTH) + 1,
                                nextCal.get(java.util.Calendar.DAY_OF_MONTH),
                                nextCal.get(java.util.Calendar.HOUR_OF_DAY),
                                nextCal.get(java.util.Calendar.MINUTE)
                            )
                        } else {
                            "无法计算"
                        }
                    }
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, tasks = filtered, nextRunTimeCache = cache) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = result.message) }
                }
            }
        }
    }

    /**
     * 按标签搜索（客户端过滤）
     * 从全部任务中筛选出 labels 包含关键词的任务
     */
    fun searchByLabel(query: String) {
        if (query.isBlank()) {
            loadTasks()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 先获取全部任务
            when (val result = taskRepository.getTasks(
                searchValue = null,
                viewFilters = null,
                viewFilterRelation = null
            )) {
                is Result.Success -> {
                    // 客户端按标签过滤
                    val filtered = result.data.filter { task ->
                        task.labels?.any { label ->
                            label.contains(query, ignoreCase = true)
                        } == true
                    }

                    val cache = mutableMapOf<Int, String>()
                    for (task in filtered) {
                        val nextCal = com.qinglong.app.util.CronParser.getNextRunTime(task.schedule, task.lastRunTime)
                        cache[task.id] = if (nextCal != null) {
                            String.format(
                                "%04d-%02d-%02d %02d:%02d",
                                nextCal.get(java.util.Calendar.YEAR),
                                nextCal.get(java.util.Calendar.MONTH) + 1,
                                nextCal.get(java.util.Calendar.DAY_OF_MONTH),
                                nextCal.get(java.util.Calendar.HOUR_OF_DAY),
                                nextCal.get(java.util.Calendar.MINUTE)
                            )
                        } else {
                            "无法计算"
                        }
                    }
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, tasks = filtered, nextRunTimeCache = cache) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = result.message) }
                }
            }
        }
    }

    /**
     * 根据当前搜索模式执行搜索
     */
    fun searchAllTasks(query: String) {
        when (_uiState.value.searchMode) {
            1 -> searchBySubscription(query)
            2 -> searchByLabel(query)
            else -> searchByName(query)
        }
    }

    fun toggleSearch() {
        _uiState.update {
            it.copy(
                showSearch = !it.showSearch,
                searchQuery = if (it.showSearch) "" else it.searchQuery
            )
        }
        if (!_uiState.value.showSearch) loadTasks()
    }

    fun toggleBatchMode() {
        _uiState.update {
            it.copy(
                isBatchMode = !it.isBatchMode,
                selectedTaskIds = emptySet()
            )
        }
    }

    fun toggleTaskSelection(taskId: Int) {
        _uiState.update {
            val newSet = it.selectedTaskIds.toMutableSet()
            if (taskId in newSet) newSet.remove(taskId) else newSet.add(taskId)
            it.copy(selectedTaskIds = newSet)
        }
    }

    fun runTask(id: Int) {
        viewModelScope.launch {
            when (taskRepository.runTasks(listOf(id))) {
                is Result.Success -> loadTasks()
                is Result.Error -> {}
            }
        }
    }

    fun stopTask(id: Int) {
        viewModelScope.launch {
            when (taskRepository.stopTasks(listOf(id))) {
                is Result.Success -> loadTasks()
                is Result.Error -> {}
            }
        }
    }

    fun pinTask(id: Int) {
        viewModelScope.launch {
            when (val result = taskRepository.pinTask(id)) {
                is Result.Success -> {
                    loadTasks()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        showErrorDialog = true,
                        errorDialogMessage = "置顶失败: ${result.message}"
                    ) }
                }
            }
        }
    }

    fun unpinTask(id: Int) {
        viewModelScope.launch {
            when (val result = taskRepository.unpinTask(id)) {
                is Result.Success -> {
                    loadTasks()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        showErrorDialog = true,
                        errorDialogMessage = "取消置顶失败: ${result.message}"
                    ) }
                }
            }
        }
    }

    fun batchRun() {
        val ids = _uiState.value.selectedTaskIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskRepository.runTasks(ids)
            _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            loadTasks()
        }
    }

    fun batchStop() {
        val ids = _uiState.value.selectedTaskIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskRepository.stopTasks(ids)
            _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            loadTasks()
        }
    }

    fun batchEnable() {
        val ids = _uiState.value.selectedTaskIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskRepository.enableTasks(ids)
            _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            loadTasks()
        }
    }

    fun batchDisable() {
        val ids = _uiState.value.selectedTaskIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskRepository.disableTasks(ids)
            _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            loadTasks()
        }
    }

    fun batchDelete() {
        val ids = _uiState.value.selectedTaskIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskRepository.deleteTasks(ids)
            _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            loadTasks()
        }
    }

    fun batchPin() {
        val ids = _uiState.value.selectedTaskIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskRepository.pinTasks(ids)
            _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            loadTasks()
        }
    }

    fun batchUnpin() {
        val ids = _uiState.value.selectedTaskIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            taskRepository.unpinTasks(ids)
            _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            loadTasks()
        }
    }

    // ========== 弹窗状态 ==========

    fun showCreateDialog() { _uiState.update { it.copy(showCreateDialog = true) } }
    fun hideCreateDialog() { _uiState.update { it.copy(showCreateDialog = false) } }

    fun showEditDialog(task: Task) {
        _uiState.update { it.copy(showEditDialog = true, editingTask = task) }
    }
    fun hideEditDialog() { _uiState.update { it.copy(showEditDialog = false, editingTask = null) } }

    /**
     * 创建新任务
     * 调用 POST /api/crons
     */
    fun createNewTask(
        name: String,
        command: String,
        schedule: String,
        labels: List<String>,
        allowMultipleInstances: Int,
        logName: String?,
        taskBefore: String?,
        taskAfter: String?
    ) {
        if (name.isBlank()) {
            _uiState.update { it.copy(showErrorDialog = true, errorDialogMessage = "任务名称不能为空") }
            return
        }
        if (command.isBlank()) {
            _uiState.update { it.copy(showErrorDialog = true, errorDialogMessage = "命令不能为空") }
            return
        }
        if (schedule.isBlank()) {
            _uiState.update { it.copy(showErrorDialog = true, errorDialogMessage = "定时规则不能为空") }
            return
        }

        // 先关闭弹窗
        _uiState.update { it.copy(showCreateDialog = false) }

        viewModelScope.launch {
            val body = mutableMapOf<String, Any>(
                "name" to name,
                "command" to command,
                "schedule" to schedule,
                "labels" to labels,
                "allow_multiple_instances" to allowMultipleInstances
            )
            if (logName != null) body["log_name"] = logName
            if (taskBefore != null) body["task_before"] = taskBefore
            if (taskAfter != null) body["task_after"] = taskAfter

            val result = taskRepository.createTask(body)
            when (result) {
                is Result.Error -> {
                    _uiState.update { it.copy(showErrorDialog = true, errorDialogMessage = "创建失败: ${result.message}") }
                }
                is Result.Success -> {
                    showToast("任务已创建")
                }
            }
            loadTasks()
        }
    }

    fun showDeleteConfirm(task: Task) {
        _uiState.update { it.copy(showDeleteConfirm = true, deletingTask = task) }
    }
    fun hideDeleteConfirm() { _uiState.update { it.copy(showDeleteConfirm = false, deletingTask = null) } }

    fun showLogDialog(task: Task) {
        // 取消之前的轮询
        logPollingJob?.cancel()

        // 重置日志状态
        _uiState.update {
            it.copy(
                showLogDialog = true,
                logTask = task,
                logContent = "",
                isLoadingLog = false,
                logFiles = emptyList(),
                isLoadingLogFiles = false,
                selectedLogFile = null,
                isShowingLogDetail = false
            )
        }

        if (task.isRunning) {
            // 运行中的任务：加载最新日志并启动定时轮询
            loadLatestLog(task.id)
            startLogPolling(task.id)
        } else {
            // 非运行中的任务：加载历史日志文件列表
            loadLogFiles(task.id)
        }
    }

    /**
     * 启动实时日志轮询（每 3 秒刷新一次）
     */
    private fun startLogPolling(taskId: Int) {
        logPollingJob?.cancel()
        logPollingJob = viewModelScope.launch {
            while (true) {
                delay(3000) // 3 秒间隔
                when (val result = taskRepository.getCronLog(taskId)) {
                    is Result.Success -> {
                        _uiState.update { it.copy(logContent = result.data ?: "") }
                    }
                    is Result.Error -> {
                    }
                }
            }
        }
    }

    private fun loadLatestLog(taskId: Int) {
        _uiState.update { it.copy(isLoadingLog = true, logContent = "") }
        viewModelScope.launch {
            when (val result = taskRepository.getCronLog(taskId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(logContent = result.data, isLoadingLog = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(logContent = "加载失败: ${result.message}", isLoadingLog = false) }
                }
            }
        }
    }

    private fun loadLogFiles(taskId: Int) {
        _uiState.update { it.copy(isLoadingLogFiles = true, logFiles = emptyList()) }
        viewModelScope.launch {
            when (val result = taskRepository.getCronLogFiles(taskId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(logFiles = result.data, isLoadingLogFiles = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingLogFiles = false) }
                }
            }
        }
    }

    fun selectLogFile(file: CronLogFile) {
        _uiState.update { it.copy(selectedLogFile = file, isShowingLogDetail = true, isLoadingLog = true, logContent = "") }
        // 加载该历史日志文件的内容
        viewModelScope.launch {
            val task = _uiState.value.logTask ?: return@launch
            when (val result = taskRepository.getCronLog(task.id)) {
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

    fun deleteLogFile() {
        val file = _uiState.value.deletingLogFile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(showDeleteLogConfirm = false) }
            when (val result = taskRepository.deleteLogFile(file.directory, file.filename)) {
                is Result.Success -> {
                    // 刷新日志列表
                    val taskId = _uiState.value.logTask?.id ?: return@launch
                    loadLogFiles(taskId)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(
                        showDeleteLogConfirm = false,
                        showErrorDialog = true,
                        errorDialogMessage = "删除日志失败: ${result.message}"
                    ) }
                }
            }
        }
    }

    fun hideLogDialog() {
        // 取消实时日志轮询
        logPollingJob?.cancel()
        logPollingJob = null
        _uiState.update {
            it.copy(
                showLogDialog = false,
                logTask = null,
                logContent = "",
                isLoadingLog = false,
                logFiles = emptyList(),
                isLoadingLogFiles = false,
                selectedLogFile = null,
                isShowingLogDetail = false,
                isLogBatchMode = false,
                selectedLogFiles = emptySet()
            )
        }
    }

    // ========== 历史日志多选 ==========

    fun toggleLogBatchMode() {
        _uiState.update {
            if (it.isLogBatchMode) {
                // 退出多选模式
                it.copy(isLogBatchMode = false, selectedLogFiles = emptySet())
            } else {
                it.copy(isLogBatchMode = true, selectedLogFiles = emptySet())
            }
        }
    }

    fun toggleLogFileSelection(file: CronLogFile) {
        _uiState.update { state ->
            val key = file.fullPath
            val newSelected = if (key in state.selectedLogFiles) {
                state.selectedLogFiles - key
            } else {
                state.selectedLogFiles + key
            }
            state.copy(selectedLogFiles = newSelected)
        }
    }

    fun selectAllLogFiles() {
        _uiState.update { state ->
            val allKeys = state.logFiles.map { it.fullPath }.toSet()
            state.copy(selectedLogFiles = allKeys)
        }
    }

    fun invertLogFileSelection() {
        _uiState.update { state ->
            val allKeys = state.logFiles.map { it.fullPath }.toSet()
            val inverted = allKeys - state.selectedLogFiles
            state.copy(selectedLogFiles = inverted)
        }
    }

    fun deleteSelectedLogFiles() {
        val files = _uiState.value.selectedLogFiles.toList()
        if (files.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLogBatchMode = false, selectedLogFiles = emptySet()) }
            var successCount = 0
            var failCount = 0
            var lastError = ""
            for (fullPath in files) {
                // 从 fullPath 解析 directory 和 filename
                val lastSlash = fullPath.lastIndexOf('/')
                val directory = if (lastSlash >= 0) fullPath.substring(0, lastSlash) else ""
                val filename = if (lastSlash >= 0) fullPath.substring(lastSlash + 1) else fullPath
                when (taskRepository.deleteLogFile(directory, filename)) {
                    is Result.Success -> successCount++
                    is Result.Error -> {
                        failCount++
                        lastError = "删除失败: ${fullPath}"
                    }
                }
            }
            // 刷新日志列表
            val taskId = _uiState.value.logTask?.id ?: return@launch
            loadLogFiles(taskId)
            if (failCount > 0) {
                _uiState.update { it.copy(
                    showErrorDialog = true,
                    errorDialogMessage = if (successCount > 0) {
                        "成功删除 $successCount 个，失败 $failCount 个。$lastError"
                    } else {
                        "删除失败 $failCount 个。$lastError"
                    }
                ) }
            }
            // 全部成功时静默刷新，不弹窗
        }
    }

    // ========== 单任务操作 ==========

    fun enableTask(id: Int) {
        viewModelScope.launch {
            taskRepository.enableTasks(listOf(id))
            loadTasks()
        }
    }

    fun disableTask(id: Int) {
        viewModelScope.launch {
            taskRepository.disableTasks(listOf(id))
            loadTasks()
        }
    }

    fun deleteTask(id: Int) {
        viewModelScope.launch {
            taskRepository.deleteTasks(listOf(id))
            loadTasks()
        }
    }

    /**
     * 保存编辑后的任务
     * 调用 PUT /api/crons
     */
    fun saveTask(
        id: Int,
        name: String,
        command: String,
        schedule: String,
        labels: List<String>,
        allowMultipleInstances: Int,
        logName: String?,
        taskBefore: String?,
        taskAfter: String?
    ) {
        if (name.isBlank()) {
            _uiState.update { it.copy(showErrorDialog = true, errorDialogMessage = "任务名称不能为空") }
            return
        }
        if (command.isBlank()) {
            _uiState.update { it.copy(showErrorDialog = true, errorDialogMessage = "命令不能为空") }
            return
        }
        if (schedule.isBlank()) {
            _uiState.update { it.copy(showErrorDialog = true, errorDialogMessage = "定时规则不能为空") }
            return
        }

        // 先关闭弹窗
        _uiState.update { it.copy(showEditDialog = false, editingTask = null) }

        viewModelScope.launch {
            val body = mutableMapOf<String, @JvmSuppressWildcards Any>(
                "id" to id,
                "name" to name,
                "command" to command,
                "schedule" to schedule,
                "labels" to labels,
                "allow_multiple_instances" to allowMultipleInstances
            )
            if (logName != null) body["log_name"] = logName
            if (taskBefore != null) body["task_before"] = taskBefore
            if (taskAfter != null) body["task_after"] = taskAfter

            val result = taskRepository.updateTask(body)
            when (result) {
                is Result.Error -> {
                    _uiState.update { it.copy(showErrorDialog = true, errorDialogMessage = "保存失败: ${result.message}") }
                }
                is Result.Success -> {
                    showToast("任务已更新")
                }
            }
            loadTasks()
        }
    }

    // ========== 视图管理 ==========

    fun showViewManager() { _uiState.update { it.copy(showViewManager = true) } }
    fun hideViewManager() { _uiState.update { it.copy(showViewManager = false, editingView = null) } }

    fun startCreateView() {
        _uiState.update { it.copy(editingView = ViewTab(0, "", 2, null, null)) }
    }

    fun startEditView(view: ViewTab) {
        _uiState.update { it.copy(editingView = view) }
    }

    fun cancelEditView() {
        _uiState.update { it.copy(editingView = null) }
    }

    fun showViewDeleteConfirm(view: ViewTab) {
        _uiState.update { it.copy(showViewDeleteConfirm = true, deletingView = view) }
    }
    fun hideViewDeleteConfirm() {
        _uiState.update { it.copy(showViewDeleteConfirm = false, deletingView = null) }
    }

    fun hideErrorDialog() {
        _uiState.update { it.copy(showErrorDialog = false, errorDialogMessage = "") }
    }

    fun saveView(name: String, filterProperty: String, filterOperation: String, filterValue: String) {
        val current = _uiState.value.editingView
        if (current == null) {
            _uiState.update { it.copy(editingView = null, showViewManager = false, error = "状态异常，请重试") }
            return
        }
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "视图名称不能为空") }
            return
        }

        // 构建 filters：API 要求必须是数组，不能传 null
        val filters = if (filterProperty.isNotBlank() && filterValue.isNotBlank()) {
            listOf(mapOf("property" to filterProperty, "operation" to filterOperation, "value" to filterValue))
        } else {
            emptyList<Map<String, Any>>()
        }

        // 先关闭弹窗，避免用户等待
        _uiState.update { it.copy(editingView = null, showViewManager = false) }

        viewModelScope.launch {
            val result = if (current.id == 0) {
                taskRepository.createView(name, filters, "and")
            } else {
                taskRepository.updateView(current.id, name, filters, "and")
            }
            if (result is Result.Error) {
                val errMsg = result.message
                _uiState.update { it.copy(showErrorDialog = true, errorDialogMessage = "保存视图失败: $errMsg") }
            }
            // 无论成功失败都重新加载视图列表
            loadViews()
        }
    }

    fun deleteView(view: ViewTab) {
        viewModelScope.launch {
            taskRepository.deleteViews(listOf(view.id))
            _uiState.update { it.copy(showViewDeleteConfirm = false, deletingView = null) }
            loadViews()
        }
    }
}
