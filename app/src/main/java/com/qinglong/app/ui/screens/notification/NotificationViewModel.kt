package com.qinglong.app.ui.screens.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.repository.NotificationRepository
import com.qinglong.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val selectedMode: String = "closed",
    val fieldValues: Map<String, String> = emptyMap(),
    val toastMessage: String? = null
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = notificationRepository.getSettings()) {
                is Result.Success -> {
                    val data = result.data
                    val mode = (data["type"] as? String) ?: "closed"
                    val values = mutableMapOf<String, String>()
                    data.forEach { (key, value) ->
                        if (key != "type" && value != null) {
                            values[key] = value.toString()
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedMode = mode,
                            fieldValues = values
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

    fun updateMode(mode: String) {
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun updateFieldValue(key: String, value: String) {
        val current = _uiState.value.fieldValues.toMutableMap()
        current[key] = value
        _uiState.update { it.copy(fieldValues = current) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            val body = state.fieldValues.toMutableMap()
            body["type"] = state.selectedMode

            when (val result = notificationRepository.updateSettings(body)) {
                is Result.Success -> {
                    val msg = if (state.selectedMode == "closed") "通知已关闭"
                    else "通知设置保存成功（测试通知已发送）"
                    _toastMessage.value = msg
                    _uiState.update { it.copy(isSaving = false) }
                }
                is Result.Error -> {
                    _toastMessage.value = "保存失败: ${result.message}"
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
