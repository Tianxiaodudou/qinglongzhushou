package com.qinglong.app.data.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val getToken: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // 登录 API 不需要 Authorization header（使用过期 Token 会导致登录失败）
        if (path.endsWith("/api/user/login")) {
            return chain.proceed(request)
        }

        val token = getToken()
        val newRequest = request.newBuilder().apply {
            if (token != null) {
                addHeader("Authorization", "Bearer $token")
            }
            addHeader("Content-Type", "application/json")
        }.build()
        return chain.proceed(newRequest)
    }
}
