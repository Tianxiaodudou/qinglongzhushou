package com.qinglong.app.data.repository

import com.qinglong.app.data.api.ApiManager
import com.qinglong.app.data.model.SystemStatus
import com.qinglong.app.data.model.SystemVersionInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemRepository @Inject constructor(
    private val apiManager: ApiManager
) {
    suspend fun getSystemStatus(): Result<SystemStatus> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.getSystemStatus()
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(resp.body()?.data!!)
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    /**
     * 获取系统版本信息
     * GET /api/system （青龙源码根接口返回 version、publishTime、branch）
     */
    suspend fun getSystemVersion(): Result<SystemVersionInfo> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.getSystemVersion()
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                val version = body.data["version"] as? String ?: ""
                val buildTime = body.data["publishTime"]?.toString()
                val branch = body.data["branch"] as? String
                Result.Success(SystemVersionInfo(version, buildTime, branch, null))
            } else {
                Result.Error(resp.code(), body?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }
}
