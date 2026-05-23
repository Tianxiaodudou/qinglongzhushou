package com.qinglong.app.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 实时运行日志记录器
 *
 * 日志文件：Download/QingLongLog-YYYYMMDD-HHmmss.txt（每次启动生成新文件）
 * Android 10+ 无需存储权限即可写入 Download 目录
 */
object LiveLogger {

    private var logFile: File? = null
    private var enabled = false
    private var contextRef: Context? = null

    fun init(context: Context) {
        contextRef = context
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
            val file = File(downloadDir, "QingLongLog-${timestamp}.txt")
            file.writeText("")
            logFile = file
            enabled = true
            i("LiveLogger", "日志文件初始化完成: ${file.absolutePath}")
        } catch (e: Exception) {
            enabled = false
            android.util.Log.e("LiveLogger", "初始化日志文件失败: ${e.message}")
        }
    }

    fun isEnabled(): Boolean = enabled
    fun getLogFilePath(): String? = logFile?.absolutePath

    fun v(tag: String, msg: String) = log("V", tag, msg)
    fun d(tag: String, msg: String) = log("D", tag, msg)
    fun i(tag: String, msg: String) = log("I", tag, msg)
    fun w(tag: String, msg: String) = log("W", tag, msg)
    fun e(tag: String, msg: String) = log("E", tag, msg)

    fun e(tag: String, msg: String, throwable: Throwable?) {
        val sb = StringBuilder()
        sb.append(formatLog("E", tag, msg))
        if (throwable != null) {
            sb.append("\n")
            sb.append(android.util.Log.getStackTraceString(throwable))
        }
        write(sb.toString())
    }

    /** 记录HTTP请求（简化版） */
    fun request(method: String, url: String, body: String? = null) {
        val sb = StringBuilder()
        sb.append(formatLog(">>>", "HTTP", "$method $url"))
        if (body != null && body.isNotEmpty()) {
            sb.append("\n").append(formatLog(">>>", "BODY", body.take(2000)))
        }
        write(sb.toString())
    }

    /** 记录HTTP响应（简化版） */
    fun response(url: String, statusCode: Int, body: String? = null) {
        val sb = StringBuilder()
        sb.append(formatLog("<<<", "HTTP", "$statusCode $url"))
        if (body != null && body.isNotEmpty()) {
            sb.append("\n").append(formatLog("<<<", "BODY", body.take(2000)))
        }
        write(sb.toString())
    }

    // ===================== 内部 =====================

    private fun log(level: String, tag: String, msg: String) {
        write(formatLog(level, tag, msg))
        when (level) {
            "V" -> android.util.Log.v(tag, msg)
            "D" -> android.util.Log.d(tag, msg)
            "I" -> android.util.Log.i(tag, msg)
            "W" -> android.util.Log.w(tag, msg)
            "E" -> android.util.Log.e(tag, msg)
        }
    }

    private fun formatLog(level: String, tag: String, msg: String): String {
        val time = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        return "$time $level/$tag: $msg"
    }

    private fun write(text: String) {
        if (!enabled) return
        try {
            logFile?.appendText("$text\n")
        } catch (e: Exception) {
            enabled = false
        }
    }
}
