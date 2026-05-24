package com.qinglong.app.ui.screens.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.model.CronLogFile
import com.qinglong.app.data.model.CronViewFilter
import com.qinglong.app.data.model.Task
import com.qinglong.app.data.model.ViewItem
import com.qinglong.app.data.repository.Result
import com.qinglong.app.data.repository.TaskRepository
import com.qinglong.app.util.LiveLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val tasks: List<Task> = emptyList(),
    val searchQuery: String = "",
    val showSearch: Boolean = false,
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
    // 视图管理弹窗
    val showViewManager: Boolean = false,
    val editingView: ViewTab? = null,
    val showViewDeleteConfirm: Boolean = false,
    val deletingView: ViewTab? = null,
    // 订阅列表（用于视图编辑）
    val subscriptions: List<com.qinglong.app.data.model.Subscription> = emptyList(),
    // 错误提示弹窗
    val showErrorDialog: Boolean = false,
    val errorDialogMessage: String = ""
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
                    LiveLogger.e("Task", "订阅列表加载失败: ${result.message}")
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
                    LiveLogger.i("Task", "视图标签加载成功: ${views.size} 个")

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
                    LiveLogger.e("Task", "视图标签加载失败: ${result.message}，使用默认标签")
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

            LiveLogger.i("Task", "加载任务: searchValue=$query, tab=${currentTab?.name}, filters=${viewFilters?.size}")

            when (val result = taskRepository.getTasks(
                searchValue = query.takeIf { it.isNotBlank() },
                viewFilters = viewFilters,
                viewFilterRelation = viewFilterRelation
            )) {
                is Result.Success -> {
                    LiveLogger.i("Task", "加载成功: ${result.data.size} 个任务")
                    _uiState.update { it.copy(isLoading = false, tasks = result.data) }
                }
                is Result.Error -> {
                    LiveLogger.e("Task", "加载失败: ${result.message}")
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) loadTasks()
    }

    /**
     * 在全部任务中按名称搜索（忽略当前视图标签）
     */
    fun searchAllTasks(query: String) {
        if (query.isBlank()) {
            loadTasks()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            LiveLogger.i("Task", "全局搜索: query=$query")

            when (val result = taskRepository.getTasks(
                searchValue = query,
                viewFilters = null,
                viewFilterRelation = null
            )) {
                is Result.Success -> {
                    LiveLogger.i("Task", "搜索成功: ${result.data.size} 个结果")
                    _uiState.update { it.copy(isLoading = false, tasks = result.data) }
                }
                is Result.Error -> {
                    LiveLogger.e("Task", "搜索失败: ${result.message}")
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
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
            LiveLogger.i("Task", "运行任务: $id")
            when (taskRepository.runTasks(listOf(id))) {
                is Result.Success -> loadTasks()
                is Result.Error -> {}
            }
        }
    }

    fun stopTask(id: Int) {
        viewModelScope.launch {
            LiveLogger.i("Task", "停止任务: $id")
            when (taskRepository.stopTasks(listOf(id))) {
                is Result.Success -> loadTasks()
                is Result.Error -> {}
            }
        }
    }

    fun batchRun() {
        val ids = _uiState.value.selectedTaskIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            LiveLogger.i("Task", "批量运行: $ids")
            taskRepository.runTasks(ids)
            _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            loadTasks()
        }
    }

    fun batchStop() {
        val ids = _uiState.value.selectedTaskIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            LiveLogger.i("Task", "批量停止: $ids")
            taskRepository.stopTasks(ids)
            _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            loadTasks()
        }
    }

    fun batchEnable() {
        val ids = _uiState.value.selectedTaskIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            LiveLogger.i("Task", "批量启用: $ids")
            taskRepository.enableTasks(ids)
            _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            loadTasks()
        }
    }

    fun batchDisable() {
        val ids = _uiState.value.selectedTaskIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            LiveLogger.i("Task", "批量禁用: $ids")
            taskRepository.disableTasks(ids)
            _uiState.update { it.copy(selectedTaskIds = emptySet()) }
            loadTasks()
        }
    }

    fun batchDelete() {
        val ids = _uiState.value.selectedTaskIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            LiveLogger.i("Task", "批量删除: $ids")
            taskRepository.deleteTasks(ids)
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
                LiveLogger.i("Task", "轮询实时日志: taskId=$taskId")
                when (val result = taskRepository.getCronLog(taskId)) {
                    is Result.Success -> {
                        _uiState.update { it.copy(logContent = result.data ?: "") }
                    }
                    is Result.Error -> {
                        LiveLogger.e("Task", "轮询日志失败: ${result.message}")
                    }
                }
            }
        }
    }

    private fun loadLatestLog(taskId: Int) {
        _uiState.update { it.copy(isLoadingLog = true, logContent = "") }
        viewModelScope.launch {
            LiveLogger.i("Task", "加载最新日志: taskId=$taskId")
            when (val result = taskRepository.getCronLog(taskId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(logContent = result.data, isLoadingLog = false) }
                }
                is Result.Error -> {
                    LiveLogger.e("Task", "加载最新日志失败: ${result.message}")
                    _uiState.update { it.copy(logContent = "加载失败: ${result.message}", isLoadingLog = false) }
                }
            }
        }
    }

    private fun loadLogFiles(taskId: Int) {
        _uiState.update { it.copy(isLoadingLogFiles = true, logFiles = emptyList()) }
        viewModelScope.launch {
            LiveLogger.i("Task", "加载历史日志列表: taskId=$taskId")
            when (val result = taskRepository.getCronLogFiles(taskId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(logFiles = result.data, isLoadingLogFiles = false) }
                }
                is Result.Error -> {
                    LiveLogger.e("Task", "加载历史日志列表失败: ${result.message}")
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
            LiveLogger.i("Task", "加载历史日志: taskId=${task.id}, file=${file.fullPath}")
            when (val result = taskRepository.getCronLog(task.id)) {
                is Result.Success -> {
                    _uiState.update { it.copy(logContent = result.data, isLoadingLog = false) }
                }
                is Result.Error -> {
                    LiveLogger.e("Task", "加载历史日志失败: ${result.message}")
                    _uiState.update { it.copy(logContent = "加载失败: ${result.message}", isLoadingLog = false) }
                }
            }
        }
    }

    fun backToLogFileList() {
        _uiState.update { it.copy(isShowingLogDetail = false, selectedLogFile = null, logContent = "") }
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
                isShowingLogDetail = false
            )
        }
    }

    // ========== 单任务操作 ==========

    fun enableTask(id: Int) {
        viewModelScope.launch {
            LiveLogger.i("Task", "启用任务: $id")
            taskRepository.enableTasks(listOf(id))
            loadTasks()
        }
    }

    fun disableTask(id: Int) {
        viewModelScope.launch {
            LiveLogger.i("Task", "禁用任务: $id")
            taskRepository.disableTasks(listOf(id))
            loadTasks()
        }
    }

    fun deleteTask(id: Int) {
        viewModelScope.launch {
            LiveLogger.i("Task", "删除任务: $id")
            taskRepository.deleteTasks(listOf(id))
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
        LiveLogger.i("Task", "saveView called: name='$name', prop='$filterProperty', op='$filterOperation', val='$filterValue', editingView=$current")
        if (current == null) {
            LiveLogger.e("Task", "saveView: editingView is null, forcing close")
            _uiState.update { it.copy(editingView = null, showViewManager = false, error = "状态异常，请重试") }
            return
        }
        if (name.isBlank()) {
            LiveLogger.e("Task", "saveView: name is blank")
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
            LiveLogger.i("Task", "saveView launching: id=${current.id}, name=$name, filters=$filters")
            val result = if (current.id == 0) {
                taskRepository.createView(name, filters, "and")
            } else {
                taskRepository.updateView(current.id, name, filters, "and")
            }
            LiveLogger.i("Task", "saveView result: $result")
            if (result is Result.Error) {
                val errMsg = result.message
                LiveLogger.e("Task", "保存视图失败: $errMsg")
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
