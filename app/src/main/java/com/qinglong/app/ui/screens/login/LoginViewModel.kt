package com.qinglong.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.api.QingLongApi
import com.qinglong.app.data.model.LoginRequest
import com.qinglong.app.data.model.ServerConfig
import com.qinglong.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    // Form fields
    val isHttps: Boolean = false,
    val domain: String = "",
    val port: String = "5700",
    val username: String = "",
    val password: String = ""
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val api: QingLongApi,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn

    fun updateHttps(enabled: Boolean) {
        _uiState.update { it.copy(isHttps = enabled) }
    }

    fun updateDomain(domain: String) {
        _uiState.update { it.copy(domain = domain, error = null) }
    }

    fun updatePort(port: String) {
        _uiState.update { it.copy(port = port, error = null) }
    }

    fun updateUsername(username: String) {
        _uiState.update { it.copy(username = username, error = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun login() {
        val state = _uiState.value
        if (state.domain.isBlank()) {
            _uiState.update { it.copy(error = "请输入服务器域名") }
            return
        }
        if (state.username.isBlank()) {
            _uiState.update { it.copy(error = "请输入用户名") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(error = "请输入密码") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val loginRequest = LoginRequest(state.username, state.password)
                val response = api.login(loginRequest)

                if (response.code == 200 && response.data != null) {
                    val token = response.data.token
                    authRepository.saveToken(token)

                    val serverConfig = ServerConfig(
                        id = UUID.randomUUID().toString(),
                        name = state.domain,
                        protocol = if (state.isHttps) "https" else "http",
                        domain = state.domain,
                        port = state.port.toIntOrNull() ?: 5700,
                        username = state.username,
                        isDefault = true
                    )
                    authRepository.saveServer(serverConfig)

                    _uiState.update { it.copy(isLoading = false, success = true) }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = response.message ?: "登录失败 (code: ${response.code})"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "网络错误: ${e.message}"
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                api.logout()
            } catch (_: Exception) { }
            authRepository.clearAuth()
            _uiState.value = LoginUiState()
        }
    }
}
