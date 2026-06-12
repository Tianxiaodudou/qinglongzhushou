package com.qinglong.app.ui.screens.othersetting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.model.SystemVersionInfo
import com.qinglong.app.data.repository.Result
import com.qinglong.app.data.repository.SystemSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OtherSettingsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val logRemoveFrequency: String = "",
    val cronConcurrency: String = "",
    val timezone: String = "",
    val globalSshKey: String = "",
    val versionInfo: String = "",
    val toastMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class OtherSettingsViewModel @Inject constructor(
    private val systemSettingsRepository: SystemSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtherSettingsUiState())
    val uiState: StateFlow<OtherSettingsUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadConfig()
    }

    fun loadConfig() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 加载版本信息
            val versionResult = systemSettingsRepository.getSystemVersion()
            val versionText = when (versionResult) {
                is Result.Success -> {
                    val v = versionResult.data
                    buildString {
                        append("青龙面板 ${v.version}")
                    }
                }
                is Result.Error -> "获取版本信息失败"
            }

            // 加载系统配置
            when (val result = systemSettingsRepository.getSystemConfig()) {
                is Result.Success -> {
                    val data = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            logRemoveFrequency = (data["logRemoveFrequency"] as? String) ?: (data["LogRemoveFrequency"] as? String) ?: "",
                            cronConcurrency = (data["cronConcurrency"] as? String) ?: (data["CronConcurrency"] as? String) ?: "",
                            timezone = (data["timezone"] as? String) ?: "",
                            globalSshKey = (data["globalSshKey"] as? String) ?: "",
                            versionInfo = versionText
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message, versionInfo = versionText)
                    }
                }
            }
        }
    }

    fun updateLogRemoveFrequency(value: String) {
        _uiState.update { it.copy(logRemoveFrequency = value) }
    }

    fun updateCronConcurrency(value: String) {
        _uiState.update { it.copy(cronConcurrency = value) }
    }

    fun updateTimezone(value: String) {
        _uiState.update { it.copy(timezone = value) }
    }

    fun updateSshKey(value: String) {
        _uiState.update { it.copy(globalSshKey = value) }
    }

    fun saveLogRemoveFrequency() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = systemSettingsRepository.updateSystemConfig(
                "logRemoveFrequency",
                _uiState.value.logRemoveFrequency
            )) {
                is Result.Success -> {
                    _toastMessage.value = "日志保留天数已保存"
                    _uiState.update { it.copy(isSaving = false) }
                }
                is Result.Error -> {
                    _toastMessage.value = "保存失败: ${result.message}"
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    fun saveCronConcurrency() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = systemSettingsRepository.updateSystemConfig(
                "cronConcurrency",
                _uiState.value.cronConcurrency
            )) {
                is Result.Success -> {
                    _toastMessage.value = "并发数已保存"
                    _uiState.update { it.copy(isSaving = false) }
                }
                is Result.Error -> {
                    _toastMessage.value = "保存失败: ${result.message}"
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    fun saveTimezone() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = systemSettingsRepository.updateSystemConfig(
                "timezone",
                _uiState.value.timezone
            )) {
                is Result.Success -> {
                    _toastMessage.value = "时区已保存"
                    _uiState.update { it.copy(isSaving = false) }
                }
                is Result.Error -> {
                    _toastMessage.value = "保存失败: ${result.message}"
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    fun saveSshKey() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = systemSettingsRepository.updateSystemConfig(
                "globalSshKey",
                _uiState.value.globalSshKey
            )) {
                is Result.Success -> {
                    _toastMessage.value = "SSH Key 已保存"
                    _uiState.update { it.copy(isSaving = false) }
                }
                is Result.Error -> {
                    _toastMessage.value = "保存失败: ${result.message}"
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    fun deleteSystemLog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = systemSettingsRepository.deleteSystemLog()) {
                is Result.Success -> {
                    _toastMessage.value = "系统日志已删除"
                    _uiState.update { it.copy(isSaving = false) }
                }
                is Result.Error -> {
                    _toastMessage.value = "删除失败: ${result.message}"
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
