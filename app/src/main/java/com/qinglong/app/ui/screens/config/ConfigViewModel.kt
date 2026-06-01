package com.qinglong.app.ui.screens.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.model.ConfigFileItem
import com.qinglong.app.data.repository.ConfigRepository
import com.qinglong.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConfigUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val files: List<ConfigFileItem> = emptyList(),
    val selectedFile: String = "config.sh",
    val content: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val toastMessage: String? = null
)

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = configRepository.getConfigFiles()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            files = result.data,
                            error = null
                        )
                    }
                    // 默认选中 config.sh 并加载内容
                    if (result.data.isNotEmpty()) {
                        selectFile("config.sh")
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

    fun selectFile(fileName: String) {
        _uiState.update { it.copy(selectedFile = fileName, content = "") }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = configRepository.getConfigDetail(fileName)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, content = result.data)
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

    fun updateContent(newContent: String) {
        _uiState.update { it.copy(content = newContent) }
    }

    fun saveConfig() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveSuccess = false) }
            when (val result = configRepository.saveConfig(state.selectedFile, state.content)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isSaving = false, saveSuccess = true, toastMessage = "保存成功")
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isSaving = false, toastMessage = "保存失败: ${result.message}")
                    }
                }
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null, saveSuccess = false) }
    }
}
