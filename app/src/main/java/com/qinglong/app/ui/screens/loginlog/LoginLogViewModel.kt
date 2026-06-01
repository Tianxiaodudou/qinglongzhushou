package com.qinglong.app.ui.screens.loginlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.model.LoginLogEntry
import com.qinglong.app.data.repository.Result
import com.qinglong.app.data.repository.SystemSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginLogUiState(
    val isLoading: Boolean = false,
    val logs: List<LoginLogEntry> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class LoginLogViewModel @Inject constructor(
    private val systemSettingsRepository: SystemSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginLogUiState())
    val uiState: StateFlow<LoginLogUiState> = _uiState.asStateFlow()

    init {
        loadLoginLog()
    }

    fun loadLoginLog() {
        viewModelScope.launch {
            _uiState.myupdate { it.copy(isLoading = true, error = null) }
            when (val result = systemSettingsRepository.getLoginLog()) {
                is Result.Success -> {
                    _uiState.myupdate {
                        it.copy(isLoading = false, logs = result.data)
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
}

private fun <T> MutableStateFlow<T>.myupdate(transform: (T) -> T) {
    value = transform(value)
}
