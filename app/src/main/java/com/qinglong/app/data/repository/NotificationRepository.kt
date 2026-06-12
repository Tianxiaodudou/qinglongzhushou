package com.qinglong.app.data.repository

import com.qinglong.app.data.api.ApiManager
import com.qinglong.app.data.api.QingLongApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val apiManager: ApiManager
) {
    private val api: QingLongApi get() = apiManager.getApi()
        ?: throw IllegalStateException("ApiManager not initialized - please login first")

    /**
     * 获取通知设置
     * GET /api/user/notification
     */
    suspend fun getSettings(): Result<Map<String, Any?>> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.getNotificationSettings()
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取通知设置失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 更新通知设置
     * PUT /api/user/notification
     * 青龙会在保存前发送测试通知
     */
    suspend fun updateSettings(body: Map<String, Any>): Result<Unit> {
        return try {
            val a = apiManager.getApi()
            if (a == null) return Result.Error(-1, "未登录")
            val resp = a.updateNotificationSettings(body)
            val b = resp.body()
            if (resp.isSuccessful && b != null && b.code == 200) {
                Result.Success(Unit)
            } else {
                // 尝试从响应体获取错误信息
                val errorBody = try {
                    resp.errorBody()?.string()
                } catch (e: Exception) { null }

                val msg = if (!errorBody.isNullOrBlank()) {
                    // 解析 JSON 错误响应
                    try {
                        val gson = com.google.gson.Gson()
                        val err = gson.fromJson(errorBody, Map::class.java)
                        err?.get("message")?.toString() ?: errorBody
                    } catch (e: Exception) {
                        errorBody
                    }
                } else {
                    b?.message ?: "通知发送失败，请检查参数"
                }
                Result.Error(b?.code ?: resp.code(), msg)
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }
}
