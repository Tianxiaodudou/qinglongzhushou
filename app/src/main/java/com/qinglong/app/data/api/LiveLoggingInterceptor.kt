package com.qinglong.app.data.api

import com.qinglong.app.util.LiveLogger
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer

/**
 * 将 HTTP 请求和响应写入 LiveLogger 的拦截器
 */
class LiveLoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // 记录请求
        val method = request.method
        val url = request.url.toString()
        val body = request.body
        val bodyString = if (body != null) {
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        } else null

        LiveLogger.request(method, url, bodyString)

        val startTime = System.currentTimeMillis()

        try {
            val response = chain.proceed(request)

            val duration = System.currentTimeMillis() - startTime
            val responseBody = response.body
            val responseString = if (responseBody != null) {
                val source = responseBody.source()
                source.request(Long.MAX_VALUE)
                val buffer = source.buffer.clone()
                buffer.readUtf8()
            } else null

            LiveLogger.response(url, response.code, responseString)
            LiveLogger.i("HTTP", "请求完成: ${response.code} (${duration}ms)")

            return response
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            LiveLogger.e("HTTP", "请求失败: ${e.message} (${duration}ms)", e)
            throw e
        }
    }
}
