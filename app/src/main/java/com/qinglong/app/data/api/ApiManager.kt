package com.qinglong.app.data.api

import com.qinglong.app.data.model.LoginRequest
import com.qinglong.app.data.repository.AuthRepository
import com.qinglong.app.di.NetworkModule
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 管理动态创建的 QingLongApi 实例
 *
 * 启动流程（由 SplashViewModel 调用 tryAutoLogin）：
 * 1. 有已保存的 Token → 创建 API 实例，调用 getUserInfo 验证
 * 2. Token 有效 → 返回 true（登录成功）
 * 3. Token 无效/没有 Token → 用保存的账号密码登录 → 获取新 Token → 创建 API 实例 → 返回 true
 * 4. 都失败 → 返回 false（需要用户手动登录）
 */
@Singleton
class ApiManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val authRepository: AuthRepository
) {
    @Volatile
    private var currentApi: QingLongApi? = null

    /** 401 认证失败回调 - 由 NavHost 注册，收到回调时退出登录并跳转登录页 */
    @Volatile
    var onUnauthorized: (() -> Unit)? = null

    /**
     * 获取当前 API 实例
     */
    fun getApi(): QingLongApi? = currentApi

    /**
     * 清除当前 API 实例
     */
    fun clearApi() {
        currentApi = null
    }

    /**
     * 创建并设置 API 实例
     */
    fun createAndSetApi(protocol: String, domain: String, port: Int): QingLongApi {
        val api = NetworkModule.createApi(protocol, domain, port, okHttpClient)
        currentApi = api
        return api
    }

    /**
     * 尝试自动登录（挂起函数，供 SplashViewModel 调用）
     * 先试 Token，失败再用账号密码
     *
     * 注意：登录请求（/api/user/login）在 AuthInterceptor 中已被排除，
     * 不会携带过期的 Authorization header，确保登录请求能正常发送。
     *
     * @return AutoLoginResult 包含是否成功和提示信息
     */
    suspend fun tryAutoLogin(): AutoLoginResult {
        val server = authRepository.loadServerConfig()
        if (server == null) return AutoLoginResult(false)

        // 方案1：有已保存的 Token → 创建 API 实例并验证
        val savedToken = authRepository.getToken()
        if (savedToken != null) {
            val api = NetworkModule.createApi(
                server.protocol, server.domain, server.port, okHttpClient
            )
            try {
                // 调用一个简单 API 验证 Token 是否有效
                val resp = api.getTasks(size = 1)
                if (resp.isSuccessful && resp.body()?.code == 200) {
                    currentApi = api
                    return AutoLoginResult(true, tokenSuccessMessage = "Token登录成功")
                }
            } catch (_: Exception) { }
            // Token 无效，继续尝试方案2
        }

        // 方案2：用账号密码登录
        val password = authRepository.getPassword()
        val username = authRepository.getUsername()
        if (password == null || username == null) {
            return AutoLoginResult(false, "Token登录失败，使用账号密码登录", "账号密码未保存")
        }

        return try {
            val tempApi = NetworkModule.createApi(
                server.protocol, server.domain, server.port, okHttpClient
            )

            val resp = tempApi.login(LoginRequest(username, password))
            val body = resp.body()
            if (resp.isSuccessful && body != null) {
                if (body.code == 420) {
                    // 面板开启了 2FA，自动登录无法完成，让用户手动登录
                    return AutoLoginResult(
                        false,
                        "Token登录失败，使用账号密码登录",
                        "该面板已开启两步验证，请手动登录"
                    )
                } else if (body.code == 200) {
                    val token = body.data?.token
                    if (token != null) {
                        authRepository.saveToken(token)
                        currentApi = NetworkModule.createApi(
                            server.protocol, server.domain, server.port, okHttpClient
                        )
                        return AutoLoginResult(true, passwordSuccessMessage = "账号密码登录成功")
                    } else {
                        return AutoLoginResult(false, "Token登录失败，使用账号密码登录", "账号密码登录失败：返回数据异常")
                    }
                }
            }
            AutoLoginResult(false, "Token登录失败，使用账号密码登录", "账号密码登录失败：${body?.message ?: "未知错误"}")
        } catch (e: Exception) {
            AutoLoginResult(false, "Token登录失败，使用账号密码登录", "账号密码登录失败：${e.message ?: "网络错误"}")
        }
    }
}

/**
 * 自动登录结果
 */
data class AutoLoginResult(
    val success: Boolean,
    val tokenFailedMessage: String? = null,
    val passwordFailedMessage: String? = null,
    val tokenSuccessMessage: String? = null,
    val passwordSuccessMessage: String? = null
)
