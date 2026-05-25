package com.qinglong.app

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.qinglong.app.ui.QingLongNavHost
import com.qinglong.app.ui.theme.QingLongTheme
import com.qinglong.app.util.CrashHandler
import com.qinglong.app.util.CrashReportDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 检查是否有崩溃日志
        val crashLogs = CrashHandler.getInstance().getCrashLogs(this)
        val latestCrashLog = if (crashLogs.isNotEmpty()) {
            crashLogs.first().readText()
        } else null

        // 清除旧的崩溃日志
        CrashHandler.getInstance().clearCrashLogs(this)

        enableEdgeToEdge()

        setContent {
            QingLongTheme {
                var showCrashDialog by remember { mutableStateOf(latestCrashLog != null) }
                val crashLog = remember { latestCrashLog }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QingLongNavHost()
                }

                // 显示崩溃日志弹窗
                if (showCrashDialog && crashLog != null) {
                    CrashReportDialog(
                        crashLog = crashLog,
                        onDismiss = { showCrashDialog = false },
                        onCopyLog = { log ->
                            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("崩溃日志", log))
                            Toast.makeText(this, "崩溃日志已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
