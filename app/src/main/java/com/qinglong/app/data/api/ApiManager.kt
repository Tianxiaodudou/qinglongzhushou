package com.qinglong.app.data.api

import com.qinglong.app.di.NetworkModule
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 管理动态创建的 QingLongApi 实例
 * 登录成功后，LoginViewModel 将正确的 API 实例设置到这里
 * 所有 Repository 通过这个类获取 API 实例
 */
@Singleton
class ApiManager @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    @Volatile
    private var currentApi: QingLongApi? = null

    /**
     * 获取当前 API 实例
     * 如果未设置（未登录），返回 null
     */
    fun getApi(): QingLongApi? = currentApi

    /**
     * 根据服务器信息创建并设置新的 API 实例
     */
    fun createAndSetApi(protocol: String, domain: String, port: Int): QingLongApi {
        val api = NetworkModule.createApi(protocol, domain, port, okHttpClient)
        currentApi = api
        return api
    }

    /**
     * 清除 API 实例（登出时调用）
     */
    fun clearApi() {
        currentApi = null
    }
}
