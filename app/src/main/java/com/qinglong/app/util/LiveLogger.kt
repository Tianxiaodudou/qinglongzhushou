package com.qinglong.app.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 实时运行日志记录器
 * 将日志写入 Download/qinglong.txt
 *
 * Android 10+：使用 MediaStore 写入，无需存储权限
 * Android 9-：使用传统文件 API 写入，需要 WRITE_EXTERNAL_STORAGE 权限
 *
 * 每次启动只保留最近一次运行的日志
 */
object LiveLogger {

    private const val LOG_FILE_NAME = "qinglong.txt"
    private var logFile: File? = null
    private var enabled = false
    private var contextRef: Context? = null

    /**
     * 初始化日志文件，每次启动覆盖旧日志
     *
     * Android 10+ 无需权限即可写入 Download 目录
     * Android 9- 需要调用方确保已有 WRITE_EXTERNAL_STORAGE 权限
     */
    fun init(context: Context) {
        contextRef = context
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+：使用 MediaStore 写入，无需权限
                initMediaStore(context)
            } else {
                // Android 9-：使用传统文件 API
                initLegacy(context)
            }
        } catch (e: Exception) {
            enabled = false
            android.util.Log.e("LiveLogger", "初始化日志文件失败: ${e.message}")
        }
    }

    /**
     * Android 10+：通过 MediaStore 写入 Download 目录
     */
    private fun initMediaStore(context: Context) {
        val contentResolver = context.contentResolver
        val downloadUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI

        // 先删除旧文件
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(LOG_FILE_NAME)
        contentResolver.delete(downloadUri, selection, selectionArgs)

        // 创建新文件
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, LOG_FILE_NAME)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = contentResolver.insert(downloadUri, contentValues)
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                // 写入空内容，清空文件
                outputStream.write("".toByteArray())
                outputStream.flush()
            }
            enabled = true
            i("LiveLogger", "日志文件初始化完成（MediaStore）: Download/$LOG_FILE_NAME")
        } else {
            throw Exception("MediaStore insert 返回 null")
        }
    }

    /**
     * Android 9-：传统文件 API
     */
    private fun initLegacy(context: Context) {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        val file = File(downloadDir, LOG_FILE_NAME)
        file.writeText("")
        logFile = file
        enabled = true
        i("LiveLogger", "日志文件初始化完成: ${file.absolutePath}")
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
        when (level) {
            "V" -> android.util.Log.v(tag, msg)
            "D" -> android.util.Log.d(tag, msg)
            "I" -> android.util.Log.i(tag, msg)
            "W" -> android.util.Log.w(tag, msg)
            "E" -> android.util.Log.e(tag, msg)
            else -> android.util.Log.d(tag, msg)
        }
    }

    private fun formatLog(level: String, tag: String, msg: String): String {
        val time = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        return "$time $level/$tag: $msg"
    }

    @Synchronized
    private fun write(text: String) {
        if (!enabled) return
        try {
            val ctx = contextRef ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+：通过 MediaStore 追加写入
                appendMediaStore(ctx, text)
            } else {
                // Android 9-：传统文件追加
                logFile?.appendText("$text\n")
            }
        } catch (e: Exception) {
            // 写入失败时静默处理，避免循环
            enabled = false
        }
    }

    /**
     * Android 10+：通过 MediaStore 追加写入
     */
    private fun appendMediaStore(context: Context, text: String) {
        val contentResolver = context.contentResolver
        val downloadUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI

        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(LOG_FILE_NAME)

        val cursor = contentResolver.query(
            downloadUri,
            arrayOf(MediaStore.MediaColumns._ID),
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                val uri = android.net.Uri.withAppendedPath(downloadUri, id.toString())
                contentResolver.openOutputStream(uri, "wa")?.use { outputStream ->
                    outputStream.write("$text\n".toByteArray())
                    outputStream.flush()
                }
            }
        }
    }
}
