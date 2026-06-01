package com.qinglong.app.data.repository

import com.qinglong.app.data.api.ApiManager
import com.qinglong.app.data.model.ApiResponse
import com.qinglong.app.data.model.ScriptItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScriptRepository @Inject constructor(
    private val apiManager: ApiManager
) {
    suspend fun getScripts(search: String? = null): Result<List<ScriptItem>> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.getScripts(search)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(resp.body()?.data ?: emptyList())
            } else {
                val httpCode = resp.code()
                val apiMsg = resp.body()?.message
                Result.Error(httpCode, apiMsg ?: "HTTP $httpCode: 获取脚本详情失败")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun getScriptDetail(key: String): Result<String> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            // 从 key 中拆分 file 和 path
            val lastSlash = key.lastIndexOf('/')
            val file = if (lastSlash >= 0) key.substring(lastSlash + 1) else key
            val path = if (lastSlash >= 0) key.substring(0, lastSlash) else ""
            val resp = api.getScriptDetail(file, path)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(resp.body()?.data ?: "")
            } else {
                val httpCode = resp.code()
                val apiMsg = resp.body()?.message
                Result.Error(httpCode, apiMsg ?: "HTTP $httpCode: 获取脚本详情失败")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun createScript(body: Map<String, Any>): Result<ScriptItem> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.createScript(body)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(resp.body()?.data!!)
            } else {
                val httpCode = resp.code()
                val apiMsg = resp.body()?.message
                Result.Error(httpCode, apiMsg ?: "HTTP $httpCode: 获取脚本详情失败")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun updateScript(body: Map<String, Any>): Result<ScriptItem> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.updateScript(body)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(resp.body()?.data!!)
            } else {
                val httpCode = resp.code()
                val apiMsg = resp.body()?.message
                Result.Error(httpCode, apiMsg ?: "HTTP $httpCode: 获取脚本详情失败")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun deleteScripts(filename: String, path: String, type: String = "file"): Result<Unit> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val body = mapOf<String, Any>(
                "filename" to filename,
                "path" to path,
                "type" to type
            )
            val resp = api.deleteScripts(body)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(Unit)
            } else {
                val httpCode = resp.code()
                val apiMsg = resp.body()?.message
                Result.Error(httpCode, apiMsg ?: "HTTP $httpCode: 删除脚本失败")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }
}
