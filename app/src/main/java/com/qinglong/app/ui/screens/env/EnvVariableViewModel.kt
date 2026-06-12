package com.qinglong.app.ui.screens.env

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.model.EnvVariable
import com.qinglong.app.data.repository.TaskRepository
import com.qinglong.app.data.repository.Result
import com.qinglong.app.util.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EnvUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val envVariables: List<EnvVariable> = emptyList(),
    val searchQuery: String = "",
    val showSearch: Boolean = false,
    // 批量操作
    val isBatchMode: Boolean = false,
    val selectedIds: Set<Int> = emptySet(),
    // 创建/编辑弹窗
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingVariable: EnvVariable? = null,
    // 删除确认
    val showDeleteConfirm: Boolean = false,
    val deletingVariable: EnvVariable? = null,
    val pageSize: Int = 10
)

@HiltViewModel
class EnvVariableViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val appSettings: AppSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnvUiState())
    val uiState: StateFlow<EnvUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        viewModelScope.launch {
            appSettings.envPageSize.collect { size ->
                _uiState.value = _uiState.value.copy(pageSize = size)
            }
        }
        // 只在首次加载时拉数据，后续页面切换复用缓存
        if (_uiState.value.envVariables.isEmpty()) {
            loadEnvVariables()
        }
    }

    fun loadEnvVariables() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = taskRepository.getEnvVariables()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, envVariables = result.data) }
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
            when (val result = taskRepository.getEnvVariables()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isRefreshing = false, envVariables = result.data) }
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
        _uiState.update { state ->
            state.copy(selectedIds = state.envVariables.map { it.id }.toSet())
        }
    }

    fun invertSelection() {
        _uiState.update {
            val allIds = it.envVariables.map { v -> v.id }.toSet()
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

    fun showEditDialog(variable: EnvVariable) {
        _uiState.update { it.copy(showEditDialog = true, editingVariable = variable) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, editingVariable = null) }
    }

    fun createEnvVariable(name: String, value: String, remarks: String) {
        viewModelScope.launch {
            val body = mapOf<String, Any>(
                "name" to name,
                "value" to value,
                "remarks" to remarks
            )
            when (val result = taskRepository.createEnvVariable(body)) {
                is Result.Success -> {
                    _toastMessage.value = "环境变量创建成功"
                    hideCreateDialog()
                    loadEnvVariables()
                }
                is Result.Error -> {
                    _toastMessage.value = "创建失败: ${result.message}"
                }
            }
        }
    }

    fun saveEdit(id: Int, name: String, value: String, remarks: String) {
        viewModelScope.launch {
            val body = mapOf<String, Any>(
                "name" to name,
                "value" to value,
                "remarks" to remarks
            )
            when (val result = taskRepository.updateEnvVariable(id, body)) {
                is Result.Success -> {
                    _toastMessage.value = "环境变量更新成功"
                    hideEditDialog()
                    loadEnvVariables()
                }
                is Result.Error -> {
                    _toastMessage.value = "更新失败: ${result.message}"
                }
            }
        }
    }

    fun showDeleteConfirm(variable: EnvVariable) {
        _uiState.update { it.copy(showDeleteConfirm = true, deletingVariable = variable) }
    }

    fun hideDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = false, deletingVariable = null) }
    }

    fun deleteEnvVariable(id: Int) {
        viewModelScope.launch {
            when (val result = taskRepository.deleteEnvVariable(id)) {
                is Result.Success -> {
                    _toastMessage.value = "环境变量已删除"
                    hideDeleteConfirm()
                    loadEnvVariables()
                }
                is Result.Error -> {
                    _toastMessage.value = "删除失败: ${result.message}"
                }
            }
        }
    }

    fun toggleStatus(id: Int, enable: Boolean) {
        viewModelScope.launch {
            when (val result = taskRepository.toggleEnvVariableStatus(id, enable)) {
                is Result.Success -> {
                    _toastMessage.value = if (enable) "环境变量已启用" else "环境变量已禁用"
                    _uiState.update { state ->
                        state.copy(envVariables = state.envVariables.map {
                            if (it.id == id) it.copy(status = if (enable) 0 else 1) else it
                        })
                    }
                }
                is Result.Error -> {
                    _toastMessage.value = "操作失败: ${result.message}"
                }
            }
        }
    }

    fun pinEnvVariable(id: Int) {
        viewModelScope.launch {
            when (val result = taskRepository.pinEnvVariable(id)) {
                is Result.Success -> {
                    _toastMessage.value = "已置顶"
                    _uiState.update { state ->
                        state.copy(envVariables = state.envVariables.map {
                            if (it.id == id) it.copy(isPinned = 1) else it
                        })
                    }
                }
                is Result.Error -> {
                    _toastMessage.value = "置顶失败: ${result.message}"
                }
            }
        }
    }

    fun unpinEnvVariable(id: Int) {
        viewModelScope.launch {
            when (val result = taskRepository.unpinEnvVariable(id)) {
                is Result.Success -> {
                    _toastMessage.value = "已取消置顶"
                    _uiState.update { state ->
                        state.copy(envVariables = state.envVariables.map {
                            if (it.id == id) it.copy(isPinned = 0) else it
                        })
                    }
                }
                is Result.Error -> {
                    _toastMessage.value = "取消置顶失败: ${result.message}"
                }
            }
        }
    }

    fun batchEnable() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds.toList()
            if (ids.isEmpty()) return@launch
            when (val result = taskRepository.batchEnableEnvVariables(ids)) {
                is Result.Success -> {
                    _toastMessage.value = "已启用 ${ids.size} 个环境变量"
                    toggleBatchMode()
                    loadEnvVariables()
                }
                is Result.Error -> {
                    _toastMessage.value = "批量启用失败: ${result.message}"
                }
            }
        }
    }

    fun batchDisable() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds.toList()
            if (ids.isEmpty()) return@launch
            when (val result = taskRepository.batchDisableEnvVariables(ids)) {
                is Result.Success -> {
                    _toastMessage.value = "已禁用 ${ids.size} 个环境变量"
                    toggleBatchMode()
                    loadEnvVariables()
                }
                is Result.Error -> {
                    _toastMessage.value = "批量禁用失败: ${result.message}"
                }
            }
        }
    }

    fun batchPin() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds.toList()
            if (ids.isEmpty()) return@launch
            when (val result = taskRepository.batchPinEnvVariables(ids)) {
                is Result.Success -> {
                    _toastMessage.value = "已置顶 ${ids.size} 个环境变量"
                    toggleBatchMode()
                    loadEnvVariables()
                }
                is Result.Error -> {
                    _toastMessage.value = "批量置顶失败: ${result.message}"
                }
            }
        }
    }

    fun batchUnpin() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds.toList()
            if (ids.isEmpty()) return@launch
            when (val result = taskRepository.batchUnpinEnvVariables(ids)) {
                is Result.Success -> {
                    _toastMessage.value = "已取消置顶 ${ids.size} 个环境变量"
                    toggleBatchMode()
                    loadEnvVariables()
                }
                is Result.Error -> {
                    _toastMessage.value = "批量取消置顶失败: ${result.message}"
                }
            }
        }
    }

    fun batchDelete() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds.toList()
            if (ids.isEmpty()) return@launch
            when (val result = taskRepository.batchDeleteEnvVariables(ids)) {
                is Result.Success -> {
                    _toastMessage.value = "已删除 ${ids.size} 个环境变量"
                    toggleBatchMode()
                    loadEnvVariables()
                }
                is Result.Error -> {
                    _toastMessage.value = "批量删除失败: ${result.message}"
                }
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
