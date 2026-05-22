package com.qinglong.app.data.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val getToken: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = getToken()
        val request = chain.request().newBuilder().apply {
            token?.let {
                addHeader("Authorization", it)
            }
            addHeader("Content-Type", "application/json")
        }.build()
        return chain.proceed(request)
    }
}
