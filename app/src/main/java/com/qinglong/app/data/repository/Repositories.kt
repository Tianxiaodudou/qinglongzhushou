package com.qinglong.app.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qinglong.app.data.api.ApiManager
import com.qinglong.app.data.api.QingLongApi
import com.qinglong.app.data.model.*
import com.qinglong.app.util.LiveLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val code: Int, val message: String) : Result<Nothing>()
}

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val serverPrefs = context.getSharedPreferences("server_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val SERVERS_KEY = "servers_json"

    init {
        migrateIfNeeded()
    }

    /**
     * 从旧版存储格式迁移到新的 JSON 数组格式
     * 旧格式：多个独立 key（server_id, server_domain, ...）
     * 新格式：servers_json = "[{...}]"
     */
    private fun migrateIfNeeded() {
        LiveLogger.i("AuthRepo", "检查是否需要迁移...")
        if (getServers().isNotEmpty()) {
            LiveLogger.i("AuthRepo", "已有新格式数据，跳过迁移")
            return
        }

        val oldDomain = serverPrefs.getString("server_domain", null)
        if (oldDomain == null) {
            LiveLogger.i("AuthRepo", "无旧格式数据，无需迁移")
            return
        }
        val oldId = serverPrefs.getString("server_id", null) ?: return

        LiveLogger.i("AuthRepo", "发现旧格式数据: $oldDomain, 开始迁移...")
        val oldPassword = securePrefs.getString("password", null)

        val oldServer = ServerConfig(
            id = oldId,
            name = serverPrefs.getString("server_name", oldDomain) ?: oldDomain,
            protocol = serverPrefs.getString("server_protocol", "http") ?: "http",
            domain = oldDomain,
            port = serverPrefs.getInt("server_port", 5700),
            username = serverPrefs.getString("server_username", "") ?: "",
            isDefault = serverPrefs.getBoolean("server_is_default", true)
        )

        serverPrefs.edit().putString(SERVERS_KEY, gson.toJson(listOf(oldServer))).apply()

        // 迁移密码到新格式 key
        if (!oldPassword.isNullOrBlank()) {
            securePrefs.edit().putString("pass_$oldId", oldPassword).apply()
            securePrefs.edit().remove("password").apply()
        }

        // 清理旧格式的独立 key
        serverPrefs.edit().apply {
            remove("server_id")
            remove("server_name")
            remove("server_protocol")
            remove("server_domain")
            remove("server_port")
            remove("server_username")
            remove("server_is_default")
        }.apply()

        LiveLogger.i("AuthRepo", "已迁移旧格式服务器配置: $oldDomain")
    }

    private val _currentServer = MutableStateFlow(getServers().firstOrNull())
    val currentServer: StateFlow<ServerConfig?> = _currentServer.asStateFlow()

    // 初始为 false（自动登录完成前不算已登录），autoLogin 完成后置为 true
    private val _autoLoginComplete = MutableStateFlow(false)
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun markAutoLoginComplete() {
        _autoLoginComplete.value = true
        // 自动登录完成后：有 token 才算已登录
        _isLoggedIn.value = getToken() != null
        LiveLogger.i("AuthRepo", "自动登录流程结束, isLoggedIn=${_isLoggedIn.value}")
    }

    fun getToken(): String? = securePrefs.getString("token", null)

    fun getPassword(serverId: String): String? = securePrefs.getString("pass_$serverId", null)

    fun getPassword(): String? {
        val s = loadCurrentServer() ?: return null
        return getPassword(s.id)
    }

    fun getUsername(): String? = loadCurrentServer()?.username

    fun saveToken(token: String) {
        securePrefs.edit().putString("token", token).apply()
    }

    fun saveCredentials(username: String, password: String) {
        val s = loadCurrentServer() ?: return
        val servers = getServers().toMutableList()
        val idx = servers.indexOfFirst { it.id == s.id }
        if (idx >= 0) {
            servers[idx] = servers[idx].copy(username = username)
            serverPrefs.edit().putString(SERVERS_KEY, gson.toJson(servers)).apply()
        }
        securePrefs.edit().putString("pass_${s.id}", password).apply()
    }

    fun getServers(): List<ServerConfig> {
        val json = serverPrefs.getString(SERVERS_KEY, null) ?: run {
            LiveLogger.i("AuthRepo", "getServers: servers_json key 不存在")
            return emptyList()
        }
        LiveLogger.i("AuthRepo", "getServers: json长度=${json.length}, 内容前80字符=${json.take(80)}")
        return try {
            val result: List<ServerConfig>? = gson.fromJson(json, object : TypeToken<List<ServerConfig>>() {}.type)
            LiveLogger.i("AuthRepo", "getServers: 解析成功, 共 ${result?.size ?: 0} 条")
            result ?: emptyList()
        } catch (e: Exception) {
            LiveLogger.e("AuthRepo", "getServers: 解析失败: ${e.message}", e)
            emptyList()
        }
    }

    fun saveServer(config: ServerConfig, password: String = "") {
        val servers = getServers().toMutableList()
        val key = "${config.protocol}://${config.domain}:${config.port}"
        servers.removeAll { "${it.protocol}://${it.domain}:${it.port}" == key }
        servers.add(0, config)
        val json = gson.toJson(servers)
        serverPrefs.edit().putString(SERVERS_KEY, json).apply()
        if (password.isNotBlank()) {
            securePrefs.edit().putString("pass_${config.id}", password).apply()
        }
        _currentServer.value = config
        _isLoggedIn.value = true
        LiveLogger.i("AuthRepo", "服务器已保存: $key, 密码${if (password.isNotBlank()) "已保存(长度${password.length})" else "未保存"}, 共 ${servers.size} 条, jsonLen=${json.length}")
    }

    fun deleteServer(serverId: String) {
        val servers = getServers().toMutableList()
        servers.removeAll { it.id == serverId }
        serverPrefs.edit().putString(SERVERS_KEY, gson.toJson(servers)).apply()
        securePrefs.edit().remove("pass_$serverId").apply()
        val next = servers.firstOrNull()
        _currentServer.value = next
        _isLoggedIn.value = next != null
    }

    fun updateServer(old: ServerConfig, new: ServerConfig) {
        val servers = getServers().toMutableList()
        val idx = servers.indexOfFirst { it.id == old.id }
        if (idx >= 0) {
            servers[idx] = new
            serverPrefs.edit().putString(SERVERS_KEY, gson.toJson(servers)).apply()
            // 如果改了域名/协议/端口导致 serverId 变了，迁移密码
            if (old.id != new.id) {
                val oldPass = getPassword(old.id)
                if (oldPass != null) {
                    securePrefs.edit().putString("pass_${new.id}", oldPass).apply()
                    securePrefs.edit().remove("pass_${old.id}").apply()
                }
            }
            _currentServer.value = new
            LiveLogger.i("AuthRepo", "服务器已更新: ${new.protocol}://${new.domain}:${new.port}")
        }
    }

    fun savePassword(serverId: String, password: String) {
        securePrefs.edit().putString("pass_$serverId", password).apply()
    }

    fun logout() {
        securePrefs.edit().remove("token").apply()
        _isLoggedIn.value = false
    }

    fun clearAuth() {
        securePrefs.edit().clear().apply()
        serverPrefs.edit().clear().apply()
        _isLoggedIn.value = false
        _currentServer.value = null
    }

    fun loadServerConfig(): ServerConfig? = getServers().firstOrNull()

    private fun loadCurrentServer(): ServerConfig? = getServers().firstOrNull()
}

@Singleton
class TaskRepository @Inject constructor(
    private val apiManager: ApiManager
) {
    private val api: QingLongApi get() = apiManager.getApi()
        ?: throw IllegalStateException("ApiManager not initialized - please login first")

    suspend fun getTasks(search: String? = null, filter: String? = null): Result<List<Task>> {
        return try {
            val api = apiManager.getApi()
            if (api == null) {
                LiveLogger.e("Task", "ApiManager.getApi() 返回 null，未登录？")
                return Result.Error(-1, "未登录，请先登录")
            }
            
            val queryObj = mutableMapOf<String, Any?>(
                "filters" to null,
                "sorts" to null,
                "filterRelation" to "and"
            )
            
            if (filter != null) {
                when (filter) {
                    "running" -> {
                        queryObj["filters"] = listOf(
                            mapOf("property" to "status", "operation" to "In", "value" to "0,0.5")
                        )
                    }
                    "stopped" -> {
                        queryObj["filters"] = listOf(
                            mapOf("property" to "isDisabled", "operation" to "In", "value" to "1")
                        )
                    }
                }
            }
            
            val queryString = com.google.gson.Gson().toJson(queryObj)
            
            LiveLogger.i("Task", "请求: searchValue=${search.orEmpty()}, filter=$filter, queryString=$queryString")
            val resp = api.getTasks(
                searchValue = search?.takeIf { it.isNotBlank() },
                page = 1,
                size = 200,
                queryString = queryString
            )
            val body = resp.body()
            LiveLogger.i("Task", "响应: code=${resp.code()}, body=${body != null}, bodyCode=${body?.code}, total=${body?.data?.total}")
            if (resp.isSuccessful && body != null && body.code == 200) {
                Result.Success(body.data?.data ?: emptyList())
            } else {
                val errMsg = "HTTP ${resp.code()} bodyCode=${body?.code} msg=${body?.message}"
                LiveLogger.e("Task", errMsg)
                Result.Error(body?.code ?: resp.code(), body?.message ?: errMsg)
            }
        } catch (e: Exception) {
            val errMsg = e.message ?: "未知错误"
            LiveLogger.e("Task", "异常: $errMsg", e)
            Result.Error(-1, errMsg)
        }
    }

    suspend fun runTask(taskId: String): Result<Unit> {
        return try {
            val resp = api.runTask(taskId)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "操作失败")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    suspend fun stopTask(taskId: String): Result<Unit> {
        return try {
            val resp = api.stopTask(taskId)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "操作失败")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    suspend fun enableTasks(ids: List<String>): Result<Unit> {
        return try {
            val resp = api.enableTasks(ids.joinToString(","))
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "操作失败")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    suspend fun disableTasks(ids: List<String>): Result<Unit> {
        return try {
            val resp = api.disableTasks(ids.joinToString(","))
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "操作失败")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            val resp = api.deleteTask(taskId)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "操作失败")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }
}
