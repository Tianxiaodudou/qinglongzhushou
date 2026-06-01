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

    // ====== Scripts (脚本管理) ======

    /**
     * 获取脚本列表
     * GET /api/scripts
     */
    suspend fun getScripts(): Result<List<ScriptItem>> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.getScripts()
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取脚本列表失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 获取脚本详情（内容）
     * GET /api/scripts/detail?file=xxx&path=xxx
     * 从 script.key 按最后一个 '/' 拆分为 file 和 path
     * 注意：API 返回的 data 是字符串（脚本内容），不是 ScriptItem 对象
     */
    suspend fun getScriptDetail(key: String): Result<String> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            // 从 key 中拆分 file 和 path
            val lastSlash = key.lastIndexOf('/')
            val file = if (lastSlash >= 0) key.substring(lastSlash + 1) else key
            val path = if (lastSlash >= 0) key.substring(0, lastSlash) else ""
            val resp = a.getScriptDetail(file, path)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                val httpCode = resp.code()
                val apiCode = body?.code
                val apiMsg = body?.message
                Result.Error(
                    apiCode ?: httpCode,
                    apiMsg ?: "HTTP $httpCode: 获取脚本详情失败"
                )
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 创建脚本
     * POST /api/scripts
     */
    suspend fun createScript(body: Map<String, Any>): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.createScript(body)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "创建脚本失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 更新脚本
     * PUT /api/scripts
     */
    suspend fun updateScript(body: Map<String, Any>): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.updateScript(body)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "更新脚本失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 删除脚本
     * DELETE /api/scripts
     * body: {filename, path, type}
     */
    suspend fun deleteScripts(filename: String, path: String, type: String = "file"): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val body = mapOf<String, Any>(
                "filename" to filename,
                "path" to path,
                "type" to type
            )
            val resp = a.deleteScripts(body)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "删除脚本失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 运行脚本
     * PUT /api/scripts/run
     * body: {filename, path, content}
     */
    suspend fun runScript(filename: String, path: String, content: String): Result<Int> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val body = mapOf<String, Any>(
                "filename" to filename,
                "path" to path,
                "content" to content
            )
            val resp = a.runScript(body)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(b.data ?: 0)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "运行脚本失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 停止运行脚本
     * PUT /api/scripts/stop
     * body: {filename, path, pid}
     */
    suspend fun stopScript(filename: String, path: String, pid: Int): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val body = mapOf<String, Any>(
                "filename" to filename,
                "path" to path,
                "pid" to pid
            )
            val resp = a.stopScript(body)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "停止脚本失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 重命名脚本
     * PUT /api/scripts/rename
     * body: {filename, path, newFilename}
     */
    suspend fun renameScript(filename: String, path: String, newFilename: String): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val body = mapOf<String, Any>(
                "filename" to filename,
                "path" to path,
                "newFilename" to newFilename
            )
            val resp = a.renameScript(body)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "重命名脚本失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    // ====== Subscriptions (订阅管理) ======

    /**
     * 获取订阅列表
     * GET /api/subscriptions
     */
    suspend fun getSubscriptions(search: String? = null): Result<List<Subscription>> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.getSubscriptions(search)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) {
                Result.Success(body.data ?: emptyList())
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取订阅列表失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 创建订阅
     * POST /api/subscriptions
     */
    suspend fun createSubscription(body: Map<String, Any>): Result<Subscription> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.createSubscription(body)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200 && b.data != null) {
                Result.Success(b.data)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "创建订阅失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 更新订阅
     * PUT /api/subscriptions
     */
    suspend fun updateSubscription(body: Map<String, Any>): Result<Subscription> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.updateSubscription(body)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200 && b.data != null) {
                Result.Success(b.data)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "更新订阅失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 删除订阅
     * DELETE /api/subscriptions (body: [id])
     */
    suspend fun deleteSubscription(id: Int, force: Boolean = false): Result<Unit> {
        return deleteSubscriptions(listOf(id), force)
    }

    /**
     * 批量删除订阅
     * DELETE /api/subscriptions (body: [ids], query: ?force=true)
     */
    suspend fun deleteSubscriptions(ids: List<Int>, force: Boolean = false): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.deleteSubscriptions(ids, force.takeIf { it })
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "删除订阅失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 运行订阅（单个）
     * 官方没有单个运行 API，统一用批量接口传单个 id
     * PUT /api/subscriptions/run (body: [id])
     */
    suspend fun runSubscription(id: Int): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.runSubscriptions(listOf(id))
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "运行订阅失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 批量启用订阅
     * PUT /api/subscriptions/enable
     */
    suspend fun batchEnableSubscriptions(ids: List<Int>): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.enableSubscriptions(ids)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "批量启用失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 批量禁用订阅
     * PUT /api/subscriptions/disable
     */
    suspend fun batchDisableSubscriptions(ids: List<Int>): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.disableSubscriptions(ids)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "批量禁用失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 批量运行订阅
     * PUT /api/subscriptions/run
     */
    suspend fun batchRunSubscriptions(ids: List<Int>): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.runSubscriptions(ids)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "批量运行失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 批量停止订阅
     * PUT /api/subscriptions/stop
     */
    suspend fun batchStopSubscriptions(ids: List<Int>): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.stopSubscriptions(ids)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "批量停止失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 获取订阅实时日志
     * 官方: GET /api/subscriptions/:id/log
     * 返回 data 是字符串
     */
    suspend fun getSubscriptionLog(subId: Int): Result<String> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.getSubscriptionLog(subId)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) {
                Result.Success(body.data ?: "")
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取订阅日志失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 获取订阅历史日志列表
     * 官方: GET /api/subscriptions/:id/logs
     * 返回 data 是日志文件列表
     */
    suspend fun getSubscriptionLogFiles(subId: Int): Result<List<CronLogFile>> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.getSubscriptionLogFiles(subId)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) {
                Result.Success(body.data ?: emptyList())
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取订阅日志列表失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    // ====== Logs (日志管理) ======

    /**
     * 获取日志文件列表
     * GET /api/logs
     */
    suspend fun getLogFiles(): Result<List<LogFile>> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.getLogFiles()
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) {
                Result.Success(body.data ?: emptyList())
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取日志列表失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 获取日志详情（历史日志文件内容）
     * 官方: GET /api/logs/detail?file=xxx&path=xxx
     * 需要 file 和 path 两个参数
     * 返回 data 是字符串（日志内容），不是对象
     */
    suspend fun getLogDetail(file: String, path: String = ""): Result<String> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.getLogDetail(file, path)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) {
                Result.Success(body.data ?: "")
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取日志详情失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    // ====== Env Variables (环境变量) ======

    /**
     * 获取环境变量列表
     * GET /api/envs
     */
    suspend fun getEnvVariables(search: String? = null): Result<List<EnvVariable>> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.getEnvVariables(search)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) {
                Result.Success(body.data ?: emptyList())
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取环境变量失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 创建环境变量
     * POST /api/envs  body: [{name, value, remarks}]
     */
    suspend fun createEnvVariable(body: Map<String, Any>): Result<EnvVariable> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.createEnvVariables(listOf(body))
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200 && b.data != null) {
                Result.Success(b.data.first())
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "创建环境变量失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 更新环境变量
     * PUT /api/envs  body: {id, name, value, remarks}
     */
    suspend fun updateEnvVariable(id: Int, body: Map<String, Any>): Result<EnvVariable> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val fullBody = body.toMutableMap()
            fullBody["id"] = id
            val resp = a.updateEnvVariable(fullBody)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200 && b.data != null) {
                Result.Success(b.data)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "更新环境变量失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 删除环境变量
     * DELETE /api/envs  body: [id]
     */
    suspend fun deleteEnvVariable(id: Int): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.deleteEnvVariables(listOf(id))
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "删除环境变量失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 切换环境变量状态（启用/禁用）
     * PUT /api/envs/enable 或 PUT /api/envs/disable  body: [id]
     */
    suspend fun toggleEnvVariableStatus(id: Int, enable: Boolean): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = if (enable) a.enableEnvVariables(listOf(id)) else a.disableEnvVariables(listOf(id))
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "切换状态失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 置顶环境变量
     * PUT /api/envs/pin  body: [id]
     */
    suspend fun pinEnvVariable(id: Int): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.pinEnvVariables(listOf(id))
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "置顶失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 取消置顶环境变量
     * PUT /api/envs/unpin  body: [id]
     */
    suspend fun unpinEnvVariable(id: Int): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.unpinEnvVariables(listOf(id))
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "取消置顶失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 批量启用环境变量
     * PUT /api/envs/enable  body: [ids]
     */
    suspend fun batchEnableEnvVariables(ids: List<Int>): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.enableEnvVariables(ids)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "批量启用失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 批量禁用环境变量
     * PUT /api/envs/disable  body: [ids]
     */
    suspend fun batchDisableEnvVariables(ids: List<Int>): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.disableEnvVariables(ids)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "批量禁用失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 批量置顶环境变量
     * PUT /api/envs/pin  body: [ids]
     */
    suspend fun batchPinEnvVariables(ids: List<Int>): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.pinEnvVariables(ids)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "批量置顶失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 批量取消置顶环境变量
     * PUT /api/envs/unpin  body: [ids]
     */
    suspend fun batchUnpinEnvVariables(ids: List<Int>): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.unpinEnvVariables(ids)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "批量取消置顶失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 批量删除环境变量
     * DELETE /api/envs  body: [ids]
     */
    suspend fun batchDeleteEnvVariables(ids: List<Int>): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.deleteEnvVariables(ids)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "批量删除失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    // ====== System (系统状态) ======

    /**
     * 获取系统状态
     * GET /api/system
     */
    suspend fun getSystemStatus(): Result<SystemStatus> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.getSystemStatus()
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取系统状态失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 获取系统版本信息
     * GET /api/system （青龙源码根接口返回 version、publishTime、branch）
     */
    suspend fun getSystemVersion(): Result<SystemVersionInfo> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.getSystemVersion()
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                val version = body.data["version"] as? String ?: ""
                val buildTime = body.data["publishTime"]?.toString()
                val branch = body.data["branch"] as? String
                Result.Success(SystemVersionInfo(version, buildTime, branch, null))
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取系统版本失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }
}

@Singleton
class ConfigRepository @Inject constructor(
    private val apiManager: ApiManager
) {
    private val api: QingLongApi get() = apiManager.getApi()
        ?: throw IllegalStateException("ApiManager not initialized - please login first")

    suspend fun getConfigFiles(): Result<List<ConfigFileItem>> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.getConfigFiles()
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取配置文件列表失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    suspend fun getConfigDetail(path: String): Result<String> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.getConfigDetail(path)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取配置文件内容失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    suspend fun saveConfig(name: String, content: String): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val bodyMap = mapOf<String, Any>("name" to name, "content" to content)
            val resp = a.saveConfig(bodyMap)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "保存配置文件失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }
}

// ===================== DependenceRepository =====================

@Singleton
class DependenceRepository @Inject constructor(
    private val apiManager: ApiManager
) {
    /**
     * 获取依赖列表
     * GET /api/dependencies
     */
    suspend fun getDependencies(
        searchValue: String? = null,
        type: String? = null,
        status: String? = null
    ): Result<List<Dependence>> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.getDependencies(searchValue, type, status)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取依赖列表失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 创建依赖
     * POST /api/dependencies
     */
    suspend fun createDependencies(body: List<Map<String, Any>>): Result<List<Dependence>> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.createDependencies(body)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200 && b.data != null) {
                Result.Success(b.data)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "创建依赖失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 更新依赖
     * PUT /api/dependencies
     */
    suspend fun updateDependence(body: Map<String, Any>): Result<Dependence> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.updateDependence(body)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200 && b.data != null) {
                Result.Success(b.data)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "更新依赖失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 删除依赖
     * DELETE /api/dependencies
     */
    suspend fun deleteDependencies(ids: List<Int>): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.deleteDependencies(ids)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "删除依赖失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 强制删除依赖
     * DELETE /api/dependencies/force
     */
    suspend fun forceDeleteDependencies(ids: List<Int>): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.forceDeleteDependencies(ids)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "强制删除依赖失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 重新安装依赖
     * PUT /api/dependencies/reinstall
     */
    suspend fun reinstallDependencies(ids: List<Int>): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.reinstallDependencies(ids)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "重新安装依赖失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 取消安装依赖
     * PUT /api/dependencies/cancel
     */
    suspend fun cancelDependencies(ids: List<Int>): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.cancelDependencies(ids)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(b?.code ?: resp.code(), b?.message ?: "取消安装依赖失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 获取依赖详情（含日志）
     * GET /api/dependencies/:id
     */
    suspend fun getDependenceDetail(id: Int): Result<Dependence> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.getDependenceDetail(id)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取依赖详情失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }
}
