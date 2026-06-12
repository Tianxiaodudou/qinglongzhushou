package com.qinglong.app.data.repository

import com.qinglong.app.data.api.ApiManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemSettingsRepository @Inject constructor(
    private val apiManager: ApiManager
) {
    /**
     * 获取系统配置
     * GET /api/system/config
     */
    suspend fun getSystemConfig(): Result<Map<String, Any>> {
        return try {
            val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
            val resp = api.getSystemConfig()
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取系统配置失败")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "网络错误")
        }
    }

    /**
     * 更新系统配置
     * PUT /api/system/config/{path}
     */
    suspend fun updateSystemConfig(path: String, value: Any): Result<Unit> {
        return try {
            val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
            val resp = api.updateSystemConfig(path, mapOf("value" to value))
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "更新配置失败")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "网络错误")
        }
    }

    /**
     * 清理依赖缓存
     * PUT /api/system/config/dependence-clean
     * @param type "node" 或 "python3"
     */
    suspend fun cleanDependenceCache(type: String): Result<Unit> {
        return try {
            val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
            val resp = api.cleanDependenceCache(mapOf("type" to type))
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "清除缓存失败")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "网络错误")
        }
    }

    /**
     * 删除系统日志
     * DELETE /api/system/log
     */
    suspend fun deleteSystemLog(): Result<Unit> {
        return try {
            val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
            val resp = api.deleteSystemLog(emptyMap<String, Any>())
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "删除系统日志失败")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "网络错误")
        }
    }

    /**
     * 获取系统版本信息
     * GET /api/system （青龙源码根接口返回 version、publishTime、branch）
     */
    suspend fun getSystemVersion(): Result<com.qinglong.app.data.model.SystemVersionInfo> {
        return try {
            val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
            val resp = api.getSystemVersion()
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                val version = body.data["version"] as? String ?: ""
                val buildTime = body.data["publishTime"]?.toString()
                val branch = body.data["branch"] as? String
                Result.Success(com.qinglong.app.data.model.SystemVersionInfo(version, buildTime, branch, null))
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取版本信息失败")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "网络错误")
        }
    }

    /**
     * 获取系统日志
     * GET /api/system/log?startTime=&endTime=
     */
    suspend fun getSystemLog(startTime: String = "", endTime: String = ""): Result<String> {
        return try {
            val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
            val resp = api.getSystemLog(startTime, endTime)
            if (resp.isSuccessful && resp.body() != null) {
                Result.Success(resp.body()!!.string())
            } else {
                Result.Error(resp.code(), "获取系统日志失败")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "网络错误")
        }
    }

    /**
     * 获取登录日志
     * GET /api/user/login-log
     */
    suspend fun getLoginLog(): Result<List<com.qinglong.app.data.model.LoginLogEntry>> {
        return try {
            val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
            val resp = api.getLoginLog()
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取登录日志失败")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "网络错误")
        }
    }
}
