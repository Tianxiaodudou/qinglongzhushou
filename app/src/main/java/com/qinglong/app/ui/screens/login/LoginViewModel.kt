package com.qinglong.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.model.LoginRequest
import com.qinglong.app.data.model.ServerConfig
import com.qinglong.app.data.repository.AuthRepository
import com.qinglong.app.di.NetworkModule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
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
    private val authRepository: AuthRepository,
    private val okHttpClient: OkHttpClient
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
                // 根据用户输入的服务器信息动态创建 API 实例
                val port = state.port.toIntOrNull() ?: 5700
                val api = NetworkModule.createApi(
                    protocol = if (state.isHttps) "https" else "http",
                    domain = state.domain,
                    port = port,
                    okHttpClient = okHttpClient
                )

                val loginRequest = LoginRequest(state.username, state.password)
                val response = api.login(loginRequest)
                val body = response.body()

                if (response.isSuccessful && body != null && body.code == 200 && body.data != null) {
                    val token = body.data.token
                    authRepository.saveToken(token)

                    val serverConfig = ServerConfig(
                        id = UUID.randomUUID().toString(),
                        name = state.domain,
                        protocol = if (state.isHttps) "https" else "http",
                        domain = state.domain,
                        port = port,
                        username = state.username,
                        isDefault = true
                    )
                    authRepository.saveServer(serverConfig)

                    _uiState.update { it.copy(isLoading = false, success = true) }
                } else {
                    val errorMsg = body?.message ?: when (response.code()) {
                        401 -> "认证失败，请检查账号密码"
                        404 -> "服务器地址错误，请检查域名和端口"
                        500 -> "服务器内部错误"
                        else -> "登录失败 (HTTP ${response.code()})"
                    }
                    _uiState.update {
                        it.copy(isLoading = false, error = errorMsg)
                    }
                }
            } catch (e: java.net.ConnectException) {
                _uiState.update {
                    it.copy(isLoading = false, error = "无法连接到服务器，请检查域名和端口")
                }
            } catch (e: java.net.SocketTimeoutException) {
                _uiState.update {
                    it.copy(isLoading = false, error = "连接超时，请检查网络或服务器状态")
                }
            } catch (e: javax.net.ssl.SSLException) {
                _uiState.update {
                    it.copy(isLoading = false, error = "SSL 连接失败，请尝试使用 HTTP")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "网络错误: ${e.localizedMessage ?: "未知错误"}")
                }
            }
        }
    }

    fun logout() {
        authRepository.clearAuth()
        _uiState.value = LoginUiState()
    }
}
