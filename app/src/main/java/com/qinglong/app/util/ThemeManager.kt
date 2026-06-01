package com.qinglong.app.util

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.qinglong.app.ui.theme.QingLongTheme
import com.qinglong.app.ui.theme.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor(
    private val appSettings: AppSettings
) {
    @Composable
    fun getThemeMode(): ThemeMode {
        val themeMode by appSettings.themeMode.collectAsState(initial = "system")
        return when (themeMode) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    @Composable
    fun isDynamicColorEnabled(): Boolean {
        val dynamicColor by appSettings.dynamicColor.collectAsState(initial = true)
        return dynamicColor
    }

    @Composable
    fun isAmoledModeEnabled(): Boolean {
        val amoledMode by appSettings.amoledMode.collectAsState(initial = false)
        return amoledMode
    }

    @Composable
    fun QingLongThemeContent(
        content: @Composable () -> Unit
    ) {
        val themeMode = getThemeMode()
        val dynamicColor = isDynamicColorEnabled()
        val amoledMode = isAmoledModeEnabled()
        val useDarkTheme = when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }

        QingLongTheme(
            darkTheme = useDarkTheme,
            dynamicColor = dynamicColor,
            amoledMode = amoledMode,
            content = content
        )
    }
}
