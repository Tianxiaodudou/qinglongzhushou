package com.qinglong.app.ui.screens.dependence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.model.Dependence
import com.qinglong.app.data.repository.DependenceRepository
import com.qinglong.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DependenceUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val dependencies: List<Dependence> = emptyList(),
    val searchQuery: String = "",
    val showSearch: Boolean = false,
    // 类型筛选
    val typeFilter: String? = null,  // null=全部, "nodejs", "python3", "linux"
    // 创建/编辑弹窗
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingDependence: Dependence? = null,
    // 日志弹窗
    val showLogDialog: Boolean = false,
    val logDependence: Dependence? = null,
    val logContent: String = "",
    val isLoadingLog: Boolean = false,
    // 删除确认
    val showDeleteConfirm: Boolean = false,
    val deletingDependence: Dependence? = null,
    // 成功消息
    val successMessage: String? = null
)

@HiltViewModel
class DependenceViewModel @Inject constructor(
    private val dependenceRepository: DependenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DependenceUiState())
    val uiState: StateFlow<DependenceUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadDependencies()
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun loadDependencies() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val state = _uiState.value
            val typeStr = state.typeFilter?.toString()
            when (val result = dependenceRepository.getDependencies(
                searchValue = state.searchQuery.takeIf { it.isNotBlank() },
                type = typeStr
            )) {
                is Result.Success -> {
                    _uiState.update { it.copy(dependencies = result.data, isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearch() {
        _uiState.update { it.copy(showSearch = !it.showSearch, searchQuery = "") }
        if (!_uiState.value.showSearch) {
            loadDependencies()
        }
    }

    fun setTypeFilter(type: String?) {
        _uiState.update { it.copy(typeFilter = type) }
        loadDependencies()
    }

    // 创建弹窗
    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun createDependencies(body: List<Map<String, Any>>) {
        viewModelScope.launch {
            when (val result = dependenceRepository.createDependencies(body)) {
                is Result.Success -> {
                    _toastMessage.value = "创建依赖成功"
                    _uiState.update { it.copy(showCreateDialog = false) }
                    loadDependencies()
                }
                is Result.Error -> {
                    _toastMessage.value = "创建依赖失败: ${result.message}"
                }
            }
        }
    }

    // 编辑弹窗
    fun showEditDialog(dependence: Dependence) {
        _uiState.update { it.copy(showEditDialog = true, editingDependence = dependence) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, editingDependence = null) }
    }

    fun updateDependence(body: Map<String, Any>) {
        viewModelScope.launch {
            when (val result = dependenceRepository.updateDependence(body)) {
                is Result.Success -> {
                    _toastMessage.value = "更新依赖成功"
                    _uiState.update { it.copy(showEditDialog = false, editingDependence = null) }
                    loadDependencies()
                }
                is Result.Error -> {
                    _toastMessage.value = "更新依赖失败: ${result.message}"
                }
            }
        }
    }

    // 删除
    fun requestDeleteDependence(dependence: Dependence) {
        _uiState.update { it.copy(showDeleteConfirm = true, deletingDependence = dependence) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteConfirm = false, deletingDependence = null) }
    }

    fun confirmDelete() {
        val dep = _uiState.value.deletingDependence ?: return
        viewModelScope.launch {
            when (val result = dependenceRepository.deleteDependencies(listOf(dep.id))) {
                is Result.Success -> {
                    _toastMessage.value = "删除依赖成功"
                    _uiState.update { it.copy(showDeleteConfirm = false, deletingDependence = null) }
                    loadDependencies()
                }
                is Result.Error -> {
                    _toastMessage.value = "删除依赖失败: ${result.message}"
                    _uiState.update { it.copy(showDeleteConfirm = false, deletingDependence = null) }
                }
            }
        }
    }

    // 重新安装
    fun reinstallDependence(dependence: Dependence) {
        viewModelScope.launch {
            when (val result = dependenceRepository.reinstallDependencies(listOf(dependence.id))) {
                is Result.Success -> {
                    _toastMessage.value = "已加入重新安装队列"
                    loadDependencies()
                }
                is Result.Error -> {
                    _toastMessage.value = "重新安装失败: ${result.message}"
                }
            }
        }
    }

    // 取消安装
    fun cancelDependence(dependence: Dependence) {
        viewModelScope.launch {
            when (val result = dependenceRepository.cancelDependencies(listOf(dependence.id))) {
                is Result.Success -> {
                    _toastMessage.value = "已取消安装"
                    loadDependencies()
                }
                is Result.Error -> {
                    _toastMessage.value = "取消安装失败: ${result.message}"
                }
            }
        }
    }

    // 日志弹窗
    fun showLogDialog(dependence: Dependence) {
        _uiState.update { it.copy(showLogDialog = true, logDependence = dependence, logContent = "", isLoadingLog = true) }
        loadDependenceLog(dependence.id)
    }

    fun hideLogDialog() {
        _uiState.update { it.copy(showLogDialog = false, logDependence = null, logContent = "", isLoadingLog = false) }
    }

    private fun loadDependenceLog(id: Int) {
        viewModelScope.launch {
            when (val result = dependenceRepository.getDependenceDetail(id)) {
                is Result.Success -> {
                    val log = (result.data.log?.joinToString("\n") ?: "") + "\n\n--- 结束 ---"
                    _uiState.update { it.copy(logContent = log, isLoadingLog = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(logContent = "加载日志失败: ${result.message}", isLoadingLog = false) }
                }
            }
        }
    }
}
