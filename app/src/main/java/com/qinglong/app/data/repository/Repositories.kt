package com.qinglong.app.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qinglong.app.data.api.ApiManager
import com.qinglong.app.data.api.QingLongApi
import com.qinglong.app.data.model.*
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
        if (getServers().isNotEmpty()) {
            return
        }

        val oldDomain = serverPrefs.getString("server_domain", null)
        if (oldDomain == null) {
            return
        }
        val oldId = serverPrefs.getString("server_id", null) ?: return
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
            return emptyList()
        }
        return try {
            val result: List<ServerConfig>? = gson.fromJson(json, object : TypeToken<List<ServerConfig>>() {}.type)
            result ?: emptyList()
        } catch (e: Exception) {
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
    private val gson = com.google.gson.Gson()

    private val api: QingLongApi get() = apiManager.getApi()
        ?: throw IllegalStateException("ApiManager not initialized - please login first")

    suspend fun getTaskViews(): Result<List<ViewItem>> {
        return try {
            val a = apiManager.getApi()
            if (a == null) {
                return Result.Error(-1, "未登录")
            }
            val resp = a.getTaskViews()
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取视图失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    suspend fun getTasks(
        searchValue: String? = null,
        viewFilters: List<CronViewFilter>? = null,
        viewFilterRelation: String? = null
    ): Result<List<Task>> {
        return try {
            val a = apiManager.getApi()
            if (a == null) {
                return Result.Error(-1, "未登录")
            }

            val queryObj = mutableMapOf<String, Any?>(
                "filters" to (viewFilters ?: emptyList<CronViewFilter>()),
                "sorts" to null,
                "filterRelation" to (viewFilterRelation ?: "and")
            )

            val queryString = gson.toJson(queryObj)
            val filtersJson = gson.toJson(emptyMap<String, String>())
            val resp = a.getTasks(
                searchValue = searchValue?.takeIf { it.isNotBlank() },
                page = 1,
                size = 200,
                filters = filtersJson,
                queryString = queryString
            )
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) {
                Result.Success(body.data?.data ?: emptyList())
            } else {
                val errMsg = "HTTP ${resp.code()} bodyCode=${body?.code} msg=${body?.message}"
                Result.Error(body?.code ?: resp.code(), body?.message ?: errMsg)
            }
        } catch (e: Exception) {
            val errMsg = e.message ?: "未知错误"
            Result.Error(-1, errMsg)
        }
    }

    suspend fun runTasks(ids: List<Int>): Result<Unit> {
        return try {
            val resp = api.runTasks(ids)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "操作失败")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    suspend fun stopTasks(ids: List<Int>): Result<Unit> {
        return try {
            val resp = api.stopTasks(ids)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "操作失败")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    suspend fun enableTasks(ids: List<Int>): Result<Unit> {
        return try {
            val resp = api.enableTasks(ids)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "操作失败")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    suspend fun disableTasks(ids: List<Int>): Result<Unit> {
        return try {
            val resp = api.disableTasks(ids)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "操作失败")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    suspend fun deleteTasks(ids: List<Int>): Result<Unit> {
        return try {
            val resp = api.deleteTasks(ids)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "操作失败")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    suspend fun pinTask(id: Int): Result<Unit> {
        return try {
            val resp = api.pinTasks(listOf(id))
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "置顶失败")
        } catch (e: Exception) {
            Result.Error(-1, "网络错误: ${e.message}")
        }
    }

    suspend fun unpinTask(id: Int): Result<Unit> {
        return try {
            val resp = api.unpinTasks(listOf(id))
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "取消置顶失败")
        } catch (e: Exception) {
            Result.Error(-1, "网络错误: ${e.message}")
        }
    }

    suspend fun pinTasks(ids: List<Int>): Result<Unit> {
        return try {
            val resp = api.pinTasks(ids)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "批量置顶失败")
        } catch (e: Exception) {
            Result.Error(-1, "网络错误: ${e.message}")
        }
    }

    suspend fun unpinTasks(ids: List<Int>): Result<Unit> {
        return try {
            val resp = api.unpinTasks(ids)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "批量取消置顶失败")
        } catch (e: Exception) {
            Result.Error(-1, "网络错误: ${e.message}")
        }
    }

    // ====== Views CRUD ======

    suspend fun createView(name: String, filters: List<Map<String, Any>>?, filterRelation: String?): Result<Unit> {
        return try {
            val body = mutableMapOf<String, Any>("name" to name)
            // API 要求 filters 必须是数组（即使是空数组），不能传 null
            body["filters"] = filters ?: emptyList<Map<String, Any>>()
            filterRelation?.let { body["filterRelation"] = it }
            val resp = api.createTaskView(body)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) Result.Success(Unit)
            else {
                val errMsg = try {
                    val errorBody = resp.errorBody()?.string()
                    if (errorBody != null) errorBody else (b?.message ?: "创建失败")
                } catch (e: Exception) {
                    b?.message ?: "创建失败"
                }
                Result.Error(b?.code ?: resp.code(), errMsg)
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    suspend fun updateView(id: Int, name: String, filters: List<Map<String, Any>>?, filterRelation: String?): Result<Unit> {
        return try {
            val body = mutableMapOf<String, Any>("id" to id, "name" to name)
            // API 要求 filters 必须是数组（即使是空数组），不能传 null
            body["filters"] = filters ?: emptyList<Map<String, Any>>()
            filterRelation?.let { body["filterRelation"] = it }
            val resp = api.updateTaskView(body)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) Result.Success(Unit)
            else {
                val errMsg = try {
                    val errorBody = resp.errorBody()?.string()
                    if (errorBody != null) errorBody else (b?.message ?: "更新失败")
                } catch (e: Exception) {
                    b?.message ?: "更新失败"
                }
                Result.Error(b?.code ?: resp.code(), errMsg)
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    suspend fun deleteViews(ids: List<Int>): Result<Unit> {
        return try {
            val resp = api.deleteTaskViews(ids)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "删除失败")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    // ====== Subscriptions ======

    suspend fun getSubscriptions(): Result<List<Subscription>> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.getSubscriptions()
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取订阅失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    // ====== Cron Log ======

    /**
     * 获取指定任务的日志内容
     * GET /api/crons/{id}/log
     */
    suspend fun getCronLog(taskId: Int): Result<String> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.getCronLog(taskId)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) {
                Result.Success(body.data ?: "")
            } else {
                val errMsg = try {
                    resp.errorBody()?.string() ?: (body?.message ?: "获取日志失败")
                } catch (e: Exception) {
                    body?.message ?: "获取日志失败"
                }
                Result.Error(body?.code ?: resp.code(), errMsg)
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 获取指定任务的历史日志文件列表
     * GET /api/crons/{id}/logs
     */
    suspend fun getCronLogFiles(taskId: Int): Result<List<CronLogFile>> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.getCronLogFiles(taskId)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取日志列表失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 删除指定路径的日志文件
     * 网页端 DELETE /api/logs 的 body: { "filename": "文件名.log", "path": "目录", "type": "file" }
     */
    suspend fun deleteLogFile(directory: String, filename: String): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.deleteLogFile(mapOf(
                "filename" to filename,
                "path" to directory,
                "type" to "file"
            ))
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "删除日志失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 更新任务
     * PUT /api/crons
     */
    suspend fun updateTask(body: Map<String, @JvmSuppressWildcards Any>): Result<Task> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.updateTask(body)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200 && b.data != null) {
                Result.Success(b.data)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "更新任务失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 创建任务
     * POST /api/crons
     */
    suspend fun createTask(body: Map<String, Any>): Result<Task> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.createTask(body)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200 && b.data != null) {
                Result.Success(b.data)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "创建任务失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }
}
