package com.qinglong.app.data.repository

import com.qinglong.app.data.api.ApiManager
import com.qinglong.app.data.api.QingLongApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppManagementRepository @Inject constructor(
    private val apiManager: ApiManager
) {
    private val api: QingLongApi get() = apiManager.getApi()
        ?: throw IllegalStateException("ApiManager not initialized - please login first")

    /**
     * 获取应用列表
     * GET /api/apps
     */
    suspend fun getApps(): Result<List<Map<String, Any?>>> {
        return try {
            val a = apiManager.getApi() ?: return Result.Error(-1, "未登录")
            val resp = a.getApps()
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取应用列表失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 创建应用
     * POST /api/apps
     */
    suspend fun createApp(name: String, scopes: List<String>): Result<Map<String, Any?>> {
        return try {
            val a = apiManager.getApi() ?: return Result.Error(-1, "未登录")
            val resp = a.createApp(mapOf("name" to name, "scopes" to scopes))
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "创建应用失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 更新应用
     * PUT /api/apps
     */
    suspend fun updateApp(id: Int, name: String, scopes: List<String>): Result<Map<String, Any?>> {
        return try {
            val a = apiManager.getApi() ?: return Result.Error(-1, "未登录")
            val resp = a.updateApp(mapOf("id" to id, "name" to name, "scopes" to scopes))
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "更新应用失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 删除应用
     * DELETE /api/apps
     */
    suspend fun deleteApp(id: Int): Result<Unit> {
        return try {
            val a = apiManager.getApi() ?: return Result.Error(-1, "未登录")
            val resp = a.deleteApps(listOf(id))
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "删除应用失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 重置应用密钥
     * PUT /api/apps/{id}/reset-secret
     */
    suspend fun resetAppSecret(id: Int): Result<Map<String, Any?>> {
        return try {
            val a = apiManager.getApi() ?: return Result.Error(-1, "未登录")
            val resp = a.resetAppSecret(id)
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "重置密钥失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }
}
