package com.qinglong.app.data.api

import com.qinglong.app.util.LiveLogger
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val getToken: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = getToken()
        val request = chain.request().newBuilder().apply {
            if (token != null) {
                addHeader("Authorization", "Bearer $token")
            } else {
                LiveLogger.w("Auth", "Token为空，请求可能返回401: ${chain.request().url}")
            }
            addHeader("Content-Type", "application/json")
        }.build()
        return chain.proceed(request)
    }
}
