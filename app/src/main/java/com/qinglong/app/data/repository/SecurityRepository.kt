package com.qinglong.app.data.repository

import com.qinglong.app.data.api.ApiManager
import com.qinglong.app.data.api.QingLongApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityRepository @Inject constructor(
    private val apiManager: ApiManager
) {
    private val api: QingLongApi get() = apiManager.getApi()
        ?: throw IllegalStateException("ApiManager not initialized - please login first")

    /**
     * 修改用户名密码
     * PUT /api/user
     */
    suspend fun updateUser(username: String, password: String): Result<Unit> {
        return try {
            val a = apiManager.getApi() ?: return Result.Error(-1, "未登录")
            val resp = a.updateUser(mapOf("username" to username, "password" to password))
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) {
                Result.Success(Unit)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "修改失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 获取两步验证初始化信息
     * GET /api/user/two-factor/init
     */
    suspend fun getTwoFactorInit(): Result<Map<String, Any?>> {
        return try {
            val a = apiManager.getApi() ?: return Result.Error(-1, "未登录")
            val resp = a.getTwoFactorInit()
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 激活两步验证
     * PUT /api/user/two-factor/active
     */
    suspend fun activateTwoFactor(code: String): Result<Boolean> {
        return try {
            val a = apiManager.getApi() ?: return Result.Error(-1, "未登录")
            val resp = a.activateTwoFactor(mapOf("code" to code))
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) {
                Result.Success(body.data ?: false)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "激活失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 停用两步验证
     * PUT /api/user/two-factor/deactive
     */
    suspend fun deactivateTwoFactor(): Result<Boolean> {
        return try {
            val a = apiManager.getApi() ?: return Result.Error(-1, "未登录")
            val resp = a.deactivateTwoFactor(emptyMap())
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200) {
                Result.Success(body.data ?: false)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "停用失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }

    /**
     * 获取用户信息（含 twoFactorActivated 状态）
     */
    suspend fun getUserInfo(): Result<QingLongApi.UserInfo> {
        return try {
            val a = apiManager.getApi() ?: return Result.Error(-1, "未登录")
            val resp = a.getUserInfo()
            val body = resp.body()
            if (resp.isSuccessful && body != null && body.code == 200 && body.data != null) {
                Result.Success(body.data)
            } else {
                Result.Error(body?.code ?: resp.code(), body?.message ?: "获取用户信息失败")
            }
        } catch (e: Exception) {
            Result.Error(-1, e.message ?: "网络错误")
        }
    }
}
