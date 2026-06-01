package com.qinglong.app.ui.screens.appmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.repository.AppManagementRepository
import com.qinglong.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppItem(
    val id: Int = 0,
    val name: String = "",
    val clientId: String = "",
    val clientSecret: String = "",
    val scopes: List<String> = emptyList()
)

data class AppManagementUiState(
    val isLoading: Boolean = false,
    val apps: List<AppItem> = emptyList(),
    val showDialog: Boolean = false,
    val editingApp: AppItem? = null,
    val dialogName: String = "",
    val dialogScopes: Set<String> = emptySet(),
    val isSaving: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val deletingApp: AppItem? = null,
    val newSecret: String? = null,
    val error: String? = null,
    val toastMessage: String? = null
)

@HiltViewModel
class AppManagementViewModel @Inject constructor(
    private val appManagementRepository: AppManagementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppManagementUiState())
    val uiState: StateFlow<AppManagementUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = appManagementRepository.getApps()) {
                is Result.Success -> {
                    val apps = result.data.map { item ->
                        AppItem(
                            id = (item["id"] as? Number)?.toInt() ?: 0,
                            name = (item["name"] as? String) ?: "",
                            clientId = (item["client_id"] as? String) ?: "",
                            clientSecret = (item["client_secret"] as? String) ?: "",
                            scopes = (item["scopes"] as? List<*>)?.map { it.toString() } ?: emptyList()
                        )
                    }
                    _uiState.update { it.copy(isLoading = false, apps = apps) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun showCreateDialog() {
        _uiState.update {
            it.copy(
                showDialog = true,
                editingApp = null,
                dialogName = "",
                dialogScopes = emptySet()
            )
        }
    }

    fun showEditDialog(app: AppItem) {
        _uiState.update {
            it.copy(
                showDialog = true,
                editingApp = app,
                dialogName = app.name,
                dialogScopes = app.scopes.toSet()
            )
        }
    }

    fun hideDialog() {
        _uiState.update { it.copy(showDialog = false, editingApp = null) }
    }

    fun updateDialogName(value: String) {
        _uiState.update { it.copy(dialogName = value) }
    }

    fun updateDialogScopes(scopes: Set<String>) {
        _uiState.update { it.copy(dialogScopes = scopes) }
    }

    fun saveApp() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            val scopesList = state.dialogScopes.toList()

            if (state.editingApp != null) {
                // 更新
                when (val result = appManagementRepository.updateApp(
                    state.editingApp.id, state.dialogName, scopesList
                )) {
                    is Result.Success -> {
                        _toastMessage.value = "更新应用成功"
                        _uiState.update { it.copy(isSaving = false, showDialog = false) }
                        loadApps()
                    }
                    is Result.Error -> {
                        _toastMessage.value = "更新失败: ${result.message}"
                        _uiState.update { it.copy(isSaving = false) }
                    }
                }
            } else {
                // 创建
                when (val result = appManagementRepository.createApp(state.dialogName, scopesList)) {
                    is Result.Success -> {
                        _toastMessage.value = "创建应用成功"
                        _uiState.update { it.copy(isSaving = false, showDialog = false) }
                        loadApps()
                    }
                    is Result.Error -> {
                        _toastMessage.value = "创建失败: ${result.message}"
                        _uiState.update { it.copy(isSaving = false) }
                    }
                }
            }
        }
    }

    fun showDeleteConfirm(app: AppItem) {
        _uiState.update { it.copy(showDeleteConfirm = true, deletingApp = app) }
    }

    fun hideDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = false, deletingApp = null) }
    }

    fun deleteApp() {
        val app = _uiState.value.deletingApp ?: return
        viewModelScope.launch {
            when (val result = appManagementRepository.deleteApp(app.id)) {
                is Result.Success -> {
                    _toastMessage.value = "删除应用成功"
                    _uiState.update { it.copy(showDeleteConfirm = false, deletingApp = null) }
                    loadApps()
                }
                is Result.Error -> {
                    _toastMessage.value = "删除失败: ${result.message}"
                    _uiState.update { it.copy(showDeleteConfirm = false, deletingApp = null) }
                }
            }
        }
    }

    fun resetAppSecret(app: AppItem) {
        viewModelScope.launch {
            when (val result = appManagementRepository.resetAppSecret(app.id)) {
                is Result.Success -> {
                    val newSecret = result.data["client_secret"] as? String ?: ""
                    _newSecret.value = newSecret
                    _toastMessage.value = "密钥已重置"
                    loadApps()
                }
                is Result.Error -> {
                    _toastMessage.value = "重置失败: ${result.message}"
                }
            }
        }
    }

    // 新密钥弹窗
    private val _newSecret = MutableStateFlow<String?>(null)
    val newSecret: StateFlow<String?> = _newSecret.asStateFlow()

    fun clearNewSecret() {
        _newSecret.value = null
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
