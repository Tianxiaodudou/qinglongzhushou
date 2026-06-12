package com.qinglong.app.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.model.LogFile
import com.qinglong.app.data.repository.Result
import com.qinglong.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val logFiles: List<LogFile> = emptyList(),
    val searchQuery: String = "",
    val showSearch: Boolean = false,
    // 日志详情
    val selectedLogFile: LogFile? = null,
    val logContent: String = "",
    val isLoadingDetail: Boolean = false,
    // 批量操作
    val isBatchMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    // 删除确认
    val showDeleteConfirm: Boolean = false,
    val deletingLogFile: LogFile? = null
)

@HiltViewModel
class LogViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadLogFiles()
    }

    fun loadLogFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = taskRepository.getLogFiles()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, logFiles = result.data) }
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
            when (val result = taskRepository.getLogFiles()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isRefreshing = false, logFiles = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isRefreshing = false, error = result.message) }
                }
            }
        }
    }

    fun loadLogDetail(logFile: LogFile) {
        _uiState.update { it.copy(selectedLogFile = logFile, isLoadingDetail = true) }
        viewModelScope.launch {
            // 官方: GET /api/logs/detail?file=xxx&path=xxx
            val file = logFile.name
            val path = logFile.path ?: ""
            when (val result = taskRepository.getLogDetail(file, path)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoadingDetail = false, logContent = result.data)
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingDetail = false) }
                    _toastMessage.value = "加载日志详情失败: ${result.message}"
                }
            }
        }
    }

    fun closeDetail() {
        _uiState.update { it.copy(selectedLogFile = null, logContent = "") }
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

    fun toggleSelection(id: String) {
        _uiState.update { state ->
            val newSelection = state.selectedIds.toMutableSet()
            if (id in newSelection) newSelection.remove(id) else newSelection.add(id)
            state.copy(selectedIds = newSelection)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedIds = state.logFiles.map { it.id }.toSet())
        }
    }

    fun deselectAll() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun invertSelection() {
        _uiState.update { state ->
            val allIds = state.logFiles.map { it.id }.toSet()
            state.copy(selectedIds = allIds - state.selectedIds)
        }
    }

    fun requestDeleteLogFile(logFile: LogFile) {
        _uiState.update { it.copy(showDeleteConfirm = true, deletingLogFile = logFile) }
    }

    fun confirmDelete() {
        val logFile = _uiState.value.deletingLogFile ?: return
        viewModelScope.launch {
            // 官方: DELETE /api/logs (body: {filename, path, type})
            val directory = logFile.path ?: ""
            val filename = logFile.name
            when (val result = taskRepository.deleteLogFile(directory, filename)) {
                is Result.Success<*> -> {
                    _toastMessage.value = "日志已删除"
                    cancelDelete()
                    loadLogFiles()
                }
                is Result.Error -> {
                    _toastMessage.value = "删除失败: ${result.message}"
                }
            }
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteConfirm = false, deletingLogFile = null) }
    }

    fun batchDelete() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) {
            _toastMessage.value = "请先选择要删除的日志"
            return
        }
        viewModelScope.launch {
            // 逐个删除（官方没有批量删除日志的 API）
            var successCount = 0
            var failCount = 0
            for (logFile in _uiState.value.logFiles) {
                if (logFile.id in ids) {
                    val directory = logFile.path ?: ""
                    val filename = logFile.name
                    when (taskRepository.deleteLogFile(directory, filename)) {
                        is Result.Success<*> -> successCount++
                        is Result.Error -> failCount++
                    }
                }
            }
            _toastMessage.value = "已删除 $successCount 个日志${if (failCount > 0) "，$failCount 个失败" else ""}"
            toggleBatchMode()
            loadLogFiles()
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
