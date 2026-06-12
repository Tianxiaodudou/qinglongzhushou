package com.qinglong.app.ui.screens.systemlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.repository.Result
import com.qinglong.app.data.repository.SystemSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SystemLogUiState(
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val logContent: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val error: String? = null,
    val deleteSuccess: Boolean = false
)

@HiltViewModel
class SystemLogViewModel @Inject constructor(
    private val systemSettingsRepository: SystemSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SystemLogUiState())
    val uiState: StateFlow<SystemLogUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        // 默认查询今天的日志
        updateTodayRange()
    }

    fun loadSystemLog() {
        viewModelScope.launch {
            _uiState.myupdate { it.copy(isLoading = true, error = null, logContent = "") }
            when (val result = systemSettingsRepository.getSystemLog(
                startTime = _uiState.value.startTime,
                endTime = _uiState.value.endTime
            )) {
                is Result.Success -> {
                    _uiState.myupdate {
                        it.copy(isLoading = false, logContent = result.data.ifEmpty { "暂无日志" })
                    }
                }
                is Result.Error -> {
                    _uiState.myupdate {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    fun refresh() {
        loadSystemLog()
    }

    fun deleteSystemLog() {
        viewModelScope.launch {
            _uiState.myupdate { it.copy(isDeleting = true) }
            when (val result = systemSettingsRepository.deleteSystemLog()) {
                is Result.Success -> {
                    _uiState.myupdate { it.copy(isDeleting = false, deleteSuccess = true) }
                    _toastMessage.value = "系统日志已删除"
                    // 删除后刷新
                    loadSystemLog()
                }
                is Result.Error -> {
                    _uiState.myupdate { it.copy(isDeleting = false) }
                    _toastMessage.value = "删除失败: ${result.message}"
                }
            }
        }
    }

    fun setStartTime(time: String) {
        _uiState.myupdate { it.copy(startTime = time) }
    }

    fun setEndTime(time: String) {
        _uiState.myupdate { it.copy(endTime = time) }
    }

    /**
     * 设置默认范围为今天
     */
    fun updateTodayRange() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())
        _uiState.myupdate {
            it.copy(startTime = "$today 00:00:00", endTime = "$today 23:59:59")
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}

private fun <T> MutableStateFlow<T>.myupdate(transform: (T) -> T) {
    value = transform(value)
}
