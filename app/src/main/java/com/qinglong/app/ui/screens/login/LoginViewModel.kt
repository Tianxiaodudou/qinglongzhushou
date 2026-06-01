package com.qinglong.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.api.ApiManager
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
    val password: String = "",
    // Two-factor auth
    val needsTwoFactor: Boolean = false,
    val twoFactorCode: String = "",
    // Server list dialog
    val showServerDialog: Boolean = false,
    val savedServers: List<ServerConfig> = emptyList(),
    // Edit server dialog
    val showEditDialog: Boolean = false,
    val editingServer: ServerConfig? = null,
    val editingPassword: String = "",
    val showDeleteConfirm: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val okHttpClient: OkHttpClient,
    private val apiManager: ApiManager
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

    fun updateTwoFactorCode(code: String) {
        if (code.length <= 6) {
            _uiState.update { it.copy(twoFactorCode = code) }
        }
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
                val protocol = if (state.isHttps) "https" else "http"
                val api = NetworkModule.createApi(
                    protocol = protocol,
                    domain = state.domain,
                    port = port,
                    okHttpClient = okHttpClient
                )

                val loginRequest = LoginRequest(state.username, state.password)
                val response = api.login(loginRequest)
                val body = response.body()

                if (response.isSuccessful && body != null) {
                    if (body.code == 420) {
                        // ===== 需要两步验证 =====
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                needsTwoFactor = true,
                                twoFactorCode = ""
                            )
                        }
                    } else if (body.code == 200 && body.data != null) {
                        // ===== 登录成功 =====
                        val token = body.data.token
                        authRepository.saveToken(token)
                        authRepository.saveCredentials(state.username, state.password)

                        apiManager.createAndSetApi(protocol, state.domain, port)

                        val serverConfig = ServerConfig(
                            id = UUID.randomUUID().toString(),
                            name = state.domain,
                            protocol = if (state.isHttps) "https" else "http",
                            domain = state.domain,
                            port = port,
                            username = state.username,
                            isDefault = true
                        )
                        authRepository.saveServer(serverConfig, password = state.password)

                        _uiState.update { it.copy(isLoading = false, success = true) }
                    } else {
                        val errorMsg = when (response.code()) {
                            401 -> "认证失败，请检查账号密码"
                            404 -> "服务器地址错误，请检查域名和端口"
                            500 -> "服务器内部错误"
                            else -> "登录失败 (HTTP ${response.code()})"
                        }
                        _uiState.update {
                            it.copy(isLoading = false, error = errorMsg)
                        }
                    }
                } else {
                    val errorMsg = when (response.code()) {
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

    /**
     * 两步验证登录
     * 先验证 2FA code，通过后返回普通登录结果
     */
    fun loginWithTwoFactor() {
        val state = _uiState.value
        if (state.twoFactorCode.length < 6) {
            _uiState.update { it.copy(error = "请输入6位验证码") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val port = state.port.toIntOrNull() ?: 5700
                val protocol = if (state.isHttps) "https" else "http"
                val api = NetworkModule.createApi(
                    protocol = protocol,
                    domain = state.domain,
                    port = port,
                    okHttpClient = okHttpClient
                )

                val body = mapOf(
                    "code" to state.twoFactorCode,
                    "username" to state.username,
                    "password" to state.password
                )
                val response = api.twoFactorLogin(body)
                val respBody = response.body()

                if (response.isSuccessful && respBody != null && respBody.code == 200 && respBody.data != null) {
                    val token = respBody.data.token
                    authRepository.saveToken(token)
                    authRepository.saveCredentials(state.username, state.password)

                    apiManager.createAndSetApi(protocol, state.domain, port)

                    val serverConfig = ServerConfig(
                        id = UUID.randomUUID().toString(),
                        name = state.domain,
                        protocol = protocol,
                        domain = state.domain,
                        port = port,
                        username = state.username,
                        isDefault = true
                    )
                    authRepository.saveServer(serverConfig, password = state.password)

                    _uiState.update { it.copy(isLoading = false, success = true) }
                } else {
                    val errorMsg = respBody?.message ?: "验证码错误，请重试"
                    _uiState.update {
                        it.copy(isLoading = false, error = errorMsg)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "网络错误: ${e.localizedMessage ?: "未知错误"}")
                }
            }
        }
    }

    fun cancelTwoFactor() {
        _uiState.update { it.copy(needsTwoFactor = false, twoFactorCode = "") }
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = LoginUiState()
    }

    fun showServerList() {
        val servers = authRepository.getServers()
        _uiState.update { it.copy(showServerDialog = true, savedServers = servers) }
    }

    fun hideServerList() {
        _uiState.update { it.copy(showServerDialog = false) }
    }

    fun selectServer(config: ServerConfig) {
        val password = authRepository.getPassword(config.id) ?: ""
        _uiState.update {
            it.copy(
                isHttps = config.protocol == "https",
                domain = config.domain,
                port = config.port.toString(),
                username = config.username,
                password = password,
                showServerDialog = false
            )
        }
    }

    fun deleteServer(config: ServerConfig) {
        authRepository.deleteServer(config.id)
        val servers = authRepository.getServers()
        _uiState.update { it.copy(savedServers = servers, showDeleteConfirm = false) }
    }

    fun showEditServer(config: ServerConfig) {
        val password = authRepository.getPassword(config.id) ?: ""
        _uiState.update {
            it.copy(
                showEditDialog = true,
                editingServer = config,
                editingPassword = password
            )
        }
    }

    fun hideEditServer() {
        _uiState.update { it.copy(showEditDialog = false, editingServer = null, editingPassword = "") }
    }

    fun confirmEditServer(
        oldServer: ServerConfig,
        newProtocol: String,
        newDomain: String,
        newPort: Int,
        newUsername: String,
        newPassword: String
    ) {
        val newConfig = oldServer.copy(
            protocol = newProtocol,
            domain = newDomain,
            port = newPort,
            username = newUsername
        )
        authRepository.updateServer(oldServer, newConfig)
        if (newPassword.isNotEmpty()) {
            authRepository.savePassword(newConfig.id, newPassword)
        }
        val servers = authRepository.getServers()
        _uiState.update {
            it.copy(
                savedServers = servers,
                showEditDialog = false,
                editingServer = null,
                editingPassword = ""
            )
        }
    }

    fun requestDeleteServer(config: ServerConfig) {
        _uiState.update { it.copy(showDeleteConfirm = true, editingServer = config) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteConfirm = false, editingServer = null) }
    }

    /**
     * 获取当前服务器配置（供 Drawer 显示用）
     */
    fun getServerConfig(): ServerConfig? = authRepository.loadServerConfig()

    /**
     * 获取青龙面板版本号（供 Drawer 显示用）
     * 使用 api/system 端点（不需要 Token 认证）
     */
    fun getQinglongVersion(): String? {
        return try {
            val config = getServerConfig() ?: return null
            val url = "${config.protocol}://${config.domain}:${config.port}/api/system"
            val json = java.net.URL(url).readText()
            @Suppress("UNCHECKED_CAST")
            val obj = com.google.gson.Gson().fromJson(json, Map::class.java) as Map<String, Any>
            @Suppress("UNCHECKED_CAST")
            val data = obj["data"] as? Map<String, Any> ?: return null
            data["version"] as? String
        } catch (e: Exception) { null }
    }

    /**
     * 选择服务器并直接登录（供 Drawer 切换服务器用）
     */
    fun selectServerAndLogin(config: ServerConfig) {
        val password = authRepository.getPassword(config.id) ?: ""
        _uiState.update {
            it.copy(
                isHttps = config.protocol == "https",
                domain = config.domain,
                port = config.port.toString(),
                username = config.username,
                password = password,
                showServerDialog = false
            )
        }
        // 直接调用 login()
        login()
    }
}
