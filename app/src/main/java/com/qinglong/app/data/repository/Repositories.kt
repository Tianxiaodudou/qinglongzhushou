package com.qinglong.app.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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

    private val authPrefs = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _currentServer = MutableStateFlow(loadCurrentServer())
    val currentServer: StateFlow<ServerConfig?> = _currentServer.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(getToken() != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun getToken(): String? = authPrefs.getString("token", null)

    fun saveToken(token: String) {
        authPrefs.edit().putString("token", token).apply()
        _isLoggedIn.value = true
    }

    fun clearAuth() {
        authPrefs.edit().clear().apply()
        _isLoggedIn.value = false
    }

    fun saveServer(config: ServerConfig) {
        authPrefs.edit().apply {
            putString("server_id", config.id)
            putString("server_name", config.name)
            putString("server_protocol", config.protocol)
            putString("server_domain", config.domain)
            putInt("server_port", config.port)
            putString("server_username", config.username)
            putBoolean("server_is_default", config.isDefault)
        }.apply()
        _currentServer.value = config
    }

    private fun loadCurrentServer(): ServerConfig? {
        val id = authPrefs.getString("server_id", null) ?: return null
        return ServerConfig(
            id = id,
            name = authPrefs.getString("server_name", "") ?: "",
            protocol = authPrefs.getString("server_protocol", "https") ?: "https",
            domain = authPrefs.getString("server_domain", "") ?: "",
            port = authPrefs.getInt("server_port", 5700),
            username = authPrefs.getString("server_username", "") ?: "",
            isDefault = authPrefs.getBoolean("server_is_default", false)
        )
    }
}

/**
 * 所有 Repository 通过 ApiManager 获取正确的 API 实例
 */
@Singleton
class TaskRepository @Inject constructor(
    private val apiManager: ApiManager
) {
    private val api: QingLongApi get() = apiManager.getApi()
        ?: throw IllegalStateException("ApiManager not initialized - please login first")

    suspend fun getTasks(search: String? = null, filter: String? = null): Result<List<Task>> {
        return try {
            val resp = api.getTasks(search, filter)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(body.data ?: emptyList())
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "Unknown error")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "Network error")
        }
    }

    suspend fun runTask(id: String): Result<Unit> {
        return try {
            val resp = api.runTask(id)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "Unknown error")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "Network error")
        }
    }

    suspend fun stopTask(id: String): Result<Unit> {
        return try {
            val resp = api.stopTask(id)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "Unknown error")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "Network error")
        }
    }

    suspend fun enableTasks(ids: List<String>): Result<Unit> {
        return try {
            val resp = api.enableTasks(ids.joinToString(","))
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "Unknown error")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "Network error")
        }
    }

    suspend fun disableTasks(ids: List<String>): Result<Unit> {
        return try {
            val resp = api.disableTasks(ids.joinToString(","))
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "Unknown error")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "Network error")
        }
    }

    suspend fun deleteTask(id: String): Result<Unit> {
        return try {
            val resp = api.deleteTask(id)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(Unit)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "Unknown error")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "Network error")
        }
    }
}

@Singleton
class SubscriptionRepository @Inject constructor(
    private val apiManager: ApiManager
) {
    private val api: QingLongApi get() = apiManager.getApi()
        ?: throw IllegalStateException("ApiManager not initialized - please login first")

    suspend fun getSubscriptions(search: String? = null): Result<List<Subscription>> {
        return try {
            val resp = api.getSubscriptions(search)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(body.data ?: emptyList())
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "Unknown error")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "Network error")
        }
    }
}

@Singleton
class LogRepository @Inject constructor(
    private val apiManager: ApiManager
) {
    private val api: QingLongApi get() = apiManager.getApi()
        ?: throw IllegalStateException("ApiManager not initialized - please login first")

    suspend fun getLogs(
        taskId: String? = null,
        search: String? = null,
        page: Int = 1,
        pageSize: Int = 50
    ): Result<List<TaskLog>> {
        return try {
            val resp = api.getLogs(taskId, search, page, pageSize)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(body.data ?: emptyList())
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "Unknown error")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "Network error")
        }
    }
}

@Singleton
class EnvVariableRepository @Inject constructor(
    private val apiManager: ApiManager
) {
    private val api: QingLongApi get() = apiManager.getApi()
        ?: throw IllegalStateException("ApiManager not initialized - please login first")

    suspend fun getEnvVariables(search: String? = null, type: String? = null): Result<List<EnvVariable>> {
        return try {
            val resp = api.getEnvVariables(search, type)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) Result.Success(body.data ?: emptyList())
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "Unknown error")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "Network error")
        }
    }
}

@Singleton
class SystemRepository @Inject constructor(
    private val apiManager: ApiManager
) {
    private val api: QingLongApi get() = apiManager.getApi()
        ?: throw IllegalStateException("ApiManager not initialized - please login first")

    suspend fun getSystemStatus(): Result<SystemStatus> {
        return try {
            val resp = api.getSystemStatus()
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) Result.Success(body.data)
            else Result.Error(body?.code ?: resp.code(), body?.message ?: "Unknown error")
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "Network error")
        }
    }
}
