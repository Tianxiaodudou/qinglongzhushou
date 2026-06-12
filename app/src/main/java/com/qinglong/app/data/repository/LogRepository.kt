package com.qinglong.app.data.repository

import com.qinglong.app.data.api.ApiManager
import com.qinglong.app.data.model.CronLogFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepository @Inject constructor(
    private val apiManager: ApiManager
) {
    suspend fun getCronLog(taskId: Int): Result<String> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.getCronLog(taskId)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(resp.body()?.data ?: "")
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    suspend fun getCronLogFiles(taskId: Int): Result<List<CronLogFile>> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.getCronLogFiles(taskId)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(resp.body()?.data ?: emptyList())
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    /**
     * 获取日志详情（内容）
     * 官方: GET /api/logs/detail?file=xxx&path=xxx
     * 返回的 data 是字符串（日志内容）
     */
    suspend fun getLogDetailByPath(file: String, path: String): Result<String> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.getLogDetail(file, path)
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(resp.body()?.data ?: "")
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }

    /**
     * 删除日志文件
     * 官方: DELETE /api/logs (body: {filename, path, type})
     */
    suspend fun deleteLogFile(directory: String, filename: String): Result<Unit> {
        val api = apiManager.getApi() ?: return Result.Error(0, "API not initialized")
        return try {
            val resp = api.deleteLogFile(mapOf(
                "filename" to filename,
                "path" to directory,
                "type" to "file"
            ))
            if (resp.isSuccessful && resp.body()?.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(resp.code(), resp.body()?.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Result.Error(0, e.message ?: "Network error")
        }
    }
}
