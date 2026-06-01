package com.qinglong.app.ui.screens.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.repository.SecurityRepository
import com.qinglong.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecurityUiState(
    val isLoading: Boolean = false,
    val username: String = "",
    val password: String = "",
    val twoFactorActivated: Boolean = false,
    val twoFactorInfo: Map<String, Any?>? = null,
    val twoFactorSecret: String = "",
    val twoFactorUrl: String = "",
    val twoFactorCode: String = "",
    val isTwoFactorSettingUp: Boolean = false,
    val isSaving: Boolean = false,
    val isTwoFactorSetupLoading: Boolean = false,
    val error: String? = null,
    val toastMessage: String? = null,
    val usernameChanged: Boolean = false
)

@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val securityRepository: SecurityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun loadUserInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = securityRepository.getUserInfo()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            username = result.data.username,
                            twoFactorActivated = false // Will be set from user info if available
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun updateTwoFactorCode(value: String) {
        _uiState.update { it.copy(twoFactorCode = value) }
    }

    fun saveUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            when (val result = securityRepository.updateUser(state.username, state.password)) {
                is Result.Success -> {
                    _toastMessage.value = "用户名密码修改成功，请重新登录"
                    _uiState.update { it.copy(isSaving = false, usernameChanged = true) }
                }
                is Result.Error -> {
                    _toastMessage.value = "修改失败: ${result.message}"
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    fun startTwoFactorSetup() {
        _uiState.update { it.copy(isTwoFactorSetupLoading = true) }
        viewModelScope.launch {
            when (val result = securityRepository.getTwoFactorInit()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            twoFactorInfo = result.data,
                            twoFactorSecret = (result.data["secret"] as? String) ?: "",
                            twoFactorUrl = (result.data["url"] as? String) ?: "",
                            isTwoFactorSettingUp = true,
                            isTwoFactorSetupLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    _toastMessage.value = "获取验证信息失败: ${result.message}"
                    _uiState.update { it.copy(isTwoFactorSetupLoading = false) }
                }
            }
        }
    }

    fun completeTwoFactorSetup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = securityRepository.activateTwoFactor(_uiState.value.twoFactorCode)) {
                is Result.Success -> {
                    if (result.data) {
                        _toastMessage.value = "两步验证已启用"
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                twoFactorActivated = true,
                                isTwoFactorSettingUp = false,
                                twoFactorCode = ""
                            )
                        }
                    } else {
                        _toastMessage.value = "验证码错误，请重试"
                        _uiState.update { it.copy(isSaving = false) }
                    }
                }
                is Result.Error -> {
                    _toastMessage.value = "启用失败: ${result.message}"
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    fun cancelTwoFactorSetup() {
        _uiState.update {
            it.copy(isTwoFactorSettingUp = false, twoFactorCode = "")
        }
    }

    fun deactivateTwoFactor() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = securityRepository.deactivateTwoFactor()) {
                is Result.Success -> {
                    _toastMessage.value = "两步验证已停用"
                    _uiState.update {
                        it.copy(isSaving = false, twoFactorActivated = false)
                    }
                }
                is Result.Error -> {
                    _toastMessage.value = "停用失败: ${result.message}"
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
