package com.qinglong.app

import android.app.Application
import com.qinglong.app.util.CrashHandler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QingLongApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 注册全局崩溃捕获
        CrashHandler.getInstance().init(this)
    }
}
