package com.qinglong.app.ui.screens.appsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qinglong.app.data.model.ServerConfig
import com.qinglong.app.data.repository.AuthRepository
import com.qinglong.app.util.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppSettingsUiState(
    val themeMode: String = "system",
    val fontScale: Int = 100,
    val taskPageSize: Int = 10,
    val subPageSize: Int = 10,
    val envPageSize: Int = 10,
    val taskLogRefreshMs: Int = 3000,
    val subLogRefreshMs: Int = 3000,
    val scriptRunRefreshMs: Int = 3000,
    // 关于
    val appVersion: String = "1.6.7",
    val qinglongVersion: String = "",
    val serverAddress: String = "",
    // 服务器管理
    val servers: List<ServerConfig> = emptyList(),
    val currentServerId: String = "",
    val isEditingServer: Boolean = false,
    val editingServer: ServerConfig? = null,
    val editingPassword: String = ""
)

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val appSettings: AppSettings,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppSettingsUiState())
    val uiState: StateFlow<AppSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appSettings.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
        viewModelScope.launch {
            appSettings.fontScale.collect { scale ->
                _uiState.value = _uiState.value.copy(fontScale = scale)
            }
        }
        viewModelScope.launch {
            appSettings.taskPageSize.collect { size ->
                _uiState.value = _uiState.value.copy(taskPageSize = size)
            }
        }
        viewModelScope.launch {
            appSettings.subPageSize.collect { size ->
                _uiState.value = _uiState.value.copy(subPageSize = size)
            }
        }
        viewModelScope.launch {
            appSettings.envPageSize.collect { size ->
                _uiState.value = _uiState.value.copy(envPageSize = size)
            }
        }
        viewModelScope.launch {
            appSettings.taskLogRefreshMs.collect { ms ->
                _uiState.value = _uiState.value.copy(taskLogRefreshMs = ms)
            }
        }
        viewModelScope.launch {
            appSettings.subLogRefreshMs.collect { ms ->
                _uiState.value = _uiState.value.copy(subLogRefreshMs = ms)
            }
        }
        viewModelScope.launch {
            appSettings.scriptRunRefreshMs.collect { ms ->
                _uiState.value = _uiState.value.copy(scriptRunRefreshMs = ms)
            }
        }
        loadExtraInfo()
    }

    private fun loadExtraInfo() {
        viewModelScope.launch {
            val servers = authRepository.getServers()
            val current = authRepository.loadServerConfig()
            _uiState.value = _uiState.value.copy(
                servers = servers,
                currentServerId = current?.id ?: ""
            )
            if (current != null) {
                _uiState.value = _uiState.value.copy(
                    serverAddress = "${current.protocol}://${current.domain}:${current.port}"
                )
            }
            // 获取青龙版本号（IO 操作）
            val version = withContext(Dispatchers.IO) {
                val config = authRepository.loadServerConfig() ?: return@withContext ""
                try {
                    val url = "${config.protocol}://${config.domain}:${config.port}/api/system"
                    val json = java.net.URL(url).readText()
                    val obj = com.google.gson.Gson().fromJson(json, Map::class.java) ?: return@withContext ""
                    val data = obj["data"] as? Map<*, *> ?: return@withContext ""
                    data["version"]?.toString() ?: ""
                } catch (_: Exception) { "" }
            }
            _uiState.value = _uiState.value.copy(qinglongVersion = version)
        }
    }

    fun refreshServers() {
        viewModelScope.launch {
            val servers = authRepository.getServers()
            val current = authRepository.loadServerConfig()
            _uiState.value = _uiState.value.copy(
                servers = servers,
                currentServerId = current?.id ?: ""
            )
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { appSettings.setThemeMode(mode) }
    }

    fun setFontScale(scale: Int) {
        viewModelScope.launch { appSettings.setFontScale(scale) }
    }

    fun setTaskPageSize(size: Int) {
        viewModelScope.launch { appSettings.setTaskPageSize(size) }
    }

    fun setSubPageSize(size: Int) {
        viewModelScope.launch { appSettings.setSubPageSize(size) }
    }

    fun setEnvPageSize(size: Int) {
        viewModelScope.launch { appSettings.setEnvPageSize(size) }
    }

    fun setTaskLogRefreshMs(ms: Int) {
        viewModelScope.launch { appSettings.setTaskLogRefreshMs(ms) }
    }

    fun setSubLogRefreshMs(ms: Int) {
        viewModelScope.launch { appSettings.setSubLogRefreshMs(ms) }
    }

    fun setScriptRunRefreshMs(ms: Int) {
        viewModelScope.launch { appSettings.setScriptRunRefreshMs(ms) }
    }

    // ===== 服务器管理 =====

    fun showEditServer(server: ServerConfig) {
        _uiState.value = _uiState.value.copy(
            isEditingServer = true,
            editingServer = server,
            editingPassword = authRepository.getPassword(server.id) ?: ""
        )
    }

    fun showAddServer() {
        val newServer = ServerConfig(
            id = java.util.UUID.randomUUID().toString(),
            name = "",
            protocol = "http",
            domain = "",
            port = 5700,
            username = "",
            isDefault = false
        )
        _uiState.value = _uiState.value.copy(
            isEditingServer = true,
            editingServer = newServer,
            editingPassword = ""
        )
    }

    fun hideEditServer() {
        _uiState.value = _uiState.value.copy(
            isEditingServer = false,
            editingServer = null,
            editingPassword = ""
        )
    }

    fun saveServer(config: ServerConfig, password: String) {
        viewModelScope.launch {
            authRepository.saveServer(config, password)
            hideEditServer()
            refreshServers()
        }
    }

    fun deleteServer(server: ServerConfig) {
        viewModelScope.launch {
            authRepository.deleteServer(server.id)
            refreshServers()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            appSettings.clearAllData()
            authRepository.clearAuth()
        }
    }
}
