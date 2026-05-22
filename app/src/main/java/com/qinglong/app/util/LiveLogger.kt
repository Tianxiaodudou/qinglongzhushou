package com.qinglong.app.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 实时运行日志记录器
 * 将日志写入 /storage/emulated/0/Download/qinglong.txt
 * 每次启动只保留最近一次运行的日志
 */
object LiveLogger {

    private const val LOG_FILE_NAME = "qinglong.txt"
    private var logFile: File? = null
    private var enabled = false

    /**
     * 初始化日志文件，每次启动覆盖旧日志
     */
    fun init(context: Context) {
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            val file = File(downloadDir, LOG_FILE_NAME)
            // 覆盖写入，只保留本次运行日志
            file.writeText("")
            logFile = file
            enabled = true
            i("LiveLogger", "日志文件初始化完成: ${file.absolutePath}")
        } catch (e: Exception) {
            enabled = false
            android.util.Log.e("LiveLogger", "初始化日志文件失败: ${e.message}")
        }
    }

    /**
     * 检查日志文件是否可用
     */
    fun isEnabled(): Boolean = enabled

    /**
     * 获取日志文件路径
     */
    fun getLogFilePath(): String? = logFile?.absolutePath

    // ===================== 日志级别 =====================

    fun v(tag: String, msg: String) = log("V", tag, msg)
    fun d(tag: String, msg: String) = log("D", tag, msg)
    fun i(tag: String, msg: String) = log("I", tag, msg)
    fun w(tag: String, msg: String) = log("W", tag, msg)
    fun e(tag: String, msg: String) = log("E", tag, msg)

    /**
     * 带异常的日志
     */
    fun e(tag: String, msg: String, throwable: Throwable?) {
        val sb = StringBuilder()
        sb.append(formatLog("E", tag, msg))
        if (throwable != null) {
            sb.append("\n")
            sb.append(android.util.Log.getStackTraceString(throwable))
        }
        write(sb.toString())
    }

    /**
     * 网络请求日志（简化版）
     */
    fun request(method: String, url: String, body: String? = null) {
        val sb = StringBuilder()
        sb.append(formatLog(">", "HTTP", "$method $url"))
        if (body != null && body.isNotEmpty()) {
            sb.append("\n")
            sb.append(formatLog(">", "BODY", body.take(2000)))
        }
        write(sb.toString())
    }

    /**
     * 网络响应日志（简化版）
     */
    fun response(url: String, statusCode: Int, body: String? = null) {
        val sb = StringBuilder()
        sb.append(formatLog("<", "HTTP", "$statusCode $url"))
        if (body != null && body.isNotEmpty()) {
            sb.append("\n")
            sb.append(formatLog("<", "BODY", body.take(2000)))
        }
        write(sb.toString())
    }

    // ===================== 内部方法 =====================

    private fun log(level: String, tag: String, msg: String) {
        write(formatLog(level, tag, msg))
        // 同时输出到 logcat
        android.util.Log.println(
            when (level) {
                "V" -> android.util.Log.VERBOSE
                "D" -> android.util.Log.DEBUG
                "I" -> android.util.Log.INFO
                "W" -> android.util.Log.WARN
                "E" -> android.util.Log.ERROR
                else -> android.util.Log.DEBUG
            },
            tag, msg
        )
    }

    private fun formatLog(level: String, tag: String, msg: String): String {
        val time = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        return "$time $level/$tag: $msg"
    }

    @Synchronized
    private fun write(text: String) {
        if (!enabled) return
        try {
            logFile?.appendText("$text\n")
        } catch (e: Exception) {
            // 写入失败时静默处理，避免循环
            enabled = false
        }
    }
}
