package com.qinglong.app

import android.app.Application
import com.qinglong.app.util.CrashHandler
import com.qinglong.app.util.LiveLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QingLongApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 注册全局崩溃捕获
        CrashHandler.getInstance().init(this)
        // 初始化实时日志（每次启动覆盖旧日志）
        LiveLogger.init(this)
    }
}
