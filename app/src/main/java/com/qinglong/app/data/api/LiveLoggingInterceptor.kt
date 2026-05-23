package com.qinglong.app.data.api

import com.qinglong.app.util.LiveLogger
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.MediaType
import okio.Buffer

/**
 * HTTP 请求/响应日志拦截器
 *
 * 只记录请求行和响应状态码，不读取响应体（避免干扰 Retrofit 反序列化）。
 * 请求体只记录 JSON 类型。
 */
class LiveLoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val method = request.method
        val url = request.url.toString()

        // 请求体日志（仅 JSON）
        val reqBody = request.body
        val reqBodyStr = if (reqBody != null && isJsonOrText(reqBody.contentType())) {
            try {
                val buffer = Buffer()
                reqBody.writeTo(buffer)
                buffer.readUtf8()
            } catch (e: Exception) {
                "<read error: ${e.message}>"
            }
        } else null

        LiveLogger.i("HTTP", ">>> $method $url${if (reqBodyStr != null) " body=$reqBodyStr" else ""}")

        val startTime = System.currentTimeMillis()

        return try {
            val response = chain.proceed(request)
            val duration = System.currentTimeMillis() - startTime
            val cl = response.body?.contentLength() ?: -1
            val ct = response.body?.contentType()?.toString() ?: "unknown"
            LiveLogger.i("HTTP", "<<< ${response.code} $method $url (${duration}ms, type=$ct, len=$cl)")
            response
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            LiveLogger.e("HTTP", "!!! 请求失败: $method $url (${duration}ms) - ${e.message}", e)
            throw e
        }
    }

    private fun isJsonOrText(contentType: MediaType?): Boolean {
        if (contentType == null) return false
        return contentType.type == "text" || contentType.subtype == "json"
    }
}
