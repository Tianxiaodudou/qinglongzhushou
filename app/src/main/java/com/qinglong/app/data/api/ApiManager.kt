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
 * 启动时用保存的账号密码自动登录获取新 token，
 * 这样每次启动都能拿到有效 token，不会 401。
 */
@Singleton
class ApiManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val authRepository: AuthRepository
) {
    @Volatile
    private var currentApi: QingLongApi? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        autoLogin()
    }

    /**
     * APP启动时：用保存的账号密码自动登录，获取新token
     */
    private fun autoLogin() {
        val server = authRepository.loadServerConfig()
        val password = authRepository.getPassword()
        val username = authRepository.getUsername()

        if (server == null || password == null || username == null) {
            return
        }

        val baseUrl = "${server.protocol}://${server.domain}:${server.port}/"

        scope.launch {
            try {
                // 创建临时 Retrofit 实例来登录
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                val tempApi = retrofit.create(QingLongApi::class.java)

                val resp = tempApi.login(LoginRequest(username, password))
                if (resp.isSuccessful && resp.body()?.code == 200) {
                    val token = resp.body()?.data?.token
                    if (token != null) {
                        authRepository.saveToken(token)
                        // 创建正式 API 实例
                        currentApi = NetworkModule.createApi(
                            server.protocol, server.domain, server.port, okHttpClient
                        )
                    }
                }
            } catch (e: Exception) {
                // 静默忽略自动登录异常
            } finally {
                authRepository.markAutoLoginComplete()
            }
        }
    }

    fun getApi(): QingLongApi? = currentApi

    fun createAndSetApi(protocol: String, domain: String, port: Int): QingLongApi {
        val api = NetworkModule.createApi(protocol, domain, port, okHttpClient)
        currentApi = api
        return api
    }

    fun clearApi() {
        currentApi = null
        scope.cancel()
    }
}
