package com.qinglong.app.util

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 全局未捕获异常处理器
 * 捕获主进程的崩溃，将日志保存到文件，下次启动时展示
 */
class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {

    private var context: Context? = null
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        this.context = context.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            saveCrashLog(throwable)
        } catch (_: Exception) {
            // 保存日志本身出错也不影响
        }

        // 把异常交给默认处理器（让系统继续崩溃流程）
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun saveCrashLog(throwable: Throwable) {
        val ctx = context ?: return
        val crashDir = File(ctx.filesDir, "crashes")
        if (!crashDir.exists()) crashDir.mkdirs()

        val timeStr = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val file = File(crashDir, "crash_$timeStr.txt")

        val sw = StringWriter()
        val pw = PrintWriter(sw)

        pw.println("========== 青龙助手崩溃日志 ==========")
        pw.println("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        pw.println("应用版本: 1.0.0")
        pw.println("======================================")
        pw.println()
        throwable.printStackTrace(pw)

        // 也打印 cause 链
        var cause = throwable.cause
        var level = 1
        while (cause != null) {
            pw.println()
            pw.println("--- Caused by (level $level) ---")
            cause.printStackTrace(pw)
            cause = cause.cause
            level++
        }

        pw.close()
        file.writeText(sw.toString())
    }

    /**
     * 检查是否有保存的崩溃日志
     */
    fun getCrashLogs(context: Context): List<File> {
        val crashDir = File(context.filesDir, "crashes")
        if (!crashDir.exists()) return emptyList()
        return crashDir.listFiles()?.sortedByDescending { it.lastModified() }?.take(5) ?: emptyList()
    }

    /**
     * 清除所有崩溃日志
     */
    fun clearCrashLogs(context: Context) {
        val crashDir = File(context.filesDir, "crashes")
        if (crashDir.exists()) {
            crashDir.listFiles()?.forEach { it.delete() }
        }
    }

    companion object {
        @Volatile
        private var instance: CrashHandler? = null

        fun getInstance(): CrashHandler {
            return instance ?: synchronized(this) {
                instance ?: CrashHandler().also { instance = it }
            }
        }
    }
}
