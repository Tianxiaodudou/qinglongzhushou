package com.qinglong.app.ui.screens.dependencesetting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.repository.Result
import com.qinglong.app.data.repository.SystemSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DependenceSettingsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val proxy: String = "",
    val nodeMirror: String = "",
    val pythonMirror: String = "",
    val linuxMirror: String = "",
    val cleanType: String = "node",
    val toastMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class DependenceSettingsViewModel @Inject constructor(
    private val systemSettingsRepository: SystemSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DependenceSettingsUiState())
    val uiState: StateFlow<DependenceSettingsUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadConfig()
    }

    fun loadConfig() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = systemSettingsRepository.getSystemConfig()) {
                is Result.Success -> {
                    val data = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            proxy = (data["proxy"] as? String) ?: "",
                            nodeMirror = (data["nodeMirror"] as? String) ?: "",
                            pythonMirror = (data["pythonMirror"] as? String) ?: "",
                            linuxMirror = (data["linuxMirror"] as? String) ?: ""
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    fun updateProxy(value: String) {
        _uiState.update { it.copy(proxy = value) }
    }

    fun updateNodeMirror(value: String) {
        _uiState.update { it.copy(nodeMirror = value) }
    }

    fun updatePythonMirror(value: String) {
        _uiState.update { it.copy(pythonMirror = value) }
    }

    fun updateLinuxMirror(value: String) {
        _uiState.update { it.copy(linuxMirror = value) }
    }

    fun updateCleanType(value: String) {
        _uiState.update { it.copy(cleanType = value) }
    }

    fun saveProxy() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = systemSettingsRepository.updateSystemConfig("proxy", _uiState.value.proxy)) {
                is Result.Success -> {
                    _toastMessage.value = "代理设置已保存"
                    _uiState.update { it.copy(isSaving = false) }
                }
                is Result.Error -> {
                    _toastMessage.value = "保存失败: ${result.message}"
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    fun saveNodeMirror() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = systemSettingsRepository.updateSystemConfig("nodeMirror", _uiState.value.nodeMirror)) {
                is Result.Success -> {
                    _toastMessage.value = "Node 镜像源已保存"
                    _uiState.update { it.copy(isSaving = false) }
                }
                is Result.Error -> {
                    _toastMessage.value = "保存失败: ${result.message}"
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    fun savePythonMirror() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = systemSettingsRepository.updateSystemConfig("pythonMirror", _uiState.value.pythonMirror)) {
                is Result.Success -> {
                    _toastMessage.value = "Python 镜像源已保存"
                    _uiState.update { it.copy(isSaving = false) }
                }
                is Result.Error -> {
                    _toastMessage.value = "保存失败: ${result.message}"
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    fun saveLinuxMirror() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = systemSettingsRepository.updateSystemConfig("linuxMirror", _uiState.value.linuxMirror)) {
                is Result.Success -> {
                    _toastMessage.value = "Linux 镜像源已保存"
                    _uiState.update { it.copy(isSaving = false) }
                }
                is Result.Error -> {
                    _toastMessage.value = "保存失败: ${result.message}"
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    fun cleanCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = systemSettingsRepository.cleanDependenceCache(_uiState.value.cleanType)) {
                is Result.Success -> {
                    _toastMessage.value = "依赖缓存已清除"
                    _uiState.update { it.copy(isSaving = false) }
                }
                is Result.Error -> {
                    _toastMessage.value = "清除失败: ${result.message}"
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
