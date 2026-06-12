package com.qinglong.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_AMOLED_MODE = booleanPreferencesKey("amoled_mode")
        private val KEY_FONT_SCALE = intPreferencesKey("font_scale")
        private val KEY_AUTO_REFRESH = booleanPreferencesKey("auto_refresh")
        private val KEY_CONFIRM_BEFORE_RUN = booleanPreferencesKey("confirm_before_run")
        private val KEY_SHOW_SYSTEM_APPS = booleanPreferencesKey("show_system_apps")
        private val KEY_LOG_CACHE_DAYS = intPreferencesKey("log_cache_days")
        // 日志刷新间隔（毫秒）
        private val KEY_TASK_LOG_REFRESH = intPreferencesKey("task_log_refresh_ms")
        private val KEY_SUB_LOG_REFRESH = intPreferencesKey("sub_log_refresh_ms")
        private val KEY_SCRIPT_RUN_REFRESH = intPreferencesKey("script_run_refresh_ms")
        private val KEY_TASK_PAGE_SIZE = intPreferencesKey("task_page_size")
        private val KEY_SUB_PAGE_SIZE = intPreferencesKey("sub_page_size")
        private val KEY_ENV_PAGE_SIZE = intPreferencesKey("env_page_size")
    }

    // 实时状态（用于 ViewModel 直接读取）
    private val _taskLogRefreshMs = MutableStateFlow(3000)
    val taskLogRefreshMsState: StateFlow<Int> = _taskLogRefreshMs.asStateFlow()

    private val _subLogRefreshMs = MutableStateFlow(3000)
    val subLogRefreshMsState: StateFlow<Int> = _subLogRefreshMs.asStateFlow()

    private val _scriptRunRefreshMs = MutableStateFlow(1000)
    val scriptRunRefreshMsState: StateFlow<Int> = _scriptRunRefreshMs.asStateFlow()

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "system"
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_COLOR] ?: true
    }

    val amoledMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AMOLED_MODE] ?: false
    }

    val fontScale: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_FONT_SCALE] ?: 100
    }

    val autoRefresh: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_REFRESH] ?: true
    }

    val taskLogRefreshMs: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_TASK_LOG_REFRESH] ?: 3000
    }

    val subLogRefreshMs: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SUB_LOG_REFRESH] ?: 3000
    }

    val scriptRunRefreshMs: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SCRIPT_RUN_REFRESH] ?: 1000
    }

    val confirmBeforeRun: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CONFIRM_BEFORE_RUN] ?: true
    }

    val showSystemApps: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_SYSTEM_APPS] ?: false
    }

    val logCacheDays: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOG_CACHE_DAYS] ?: 7
    }

    val taskPageSize: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_TASK_PAGE_SIZE] ?: 10
    }

    val subPageSize: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SUB_PAGE_SIZE] ?: 10
    }

    val envPageSize: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_ENV_PAGE_SIZE] ?: 10
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[KEY_THEME_MODE] = mode }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_AMOLED_MODE] = enabled }
    }

    suspend fun setFontScale(scale: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_FONT_SCALE] = scale }
    }

    suspend fun setAutoRefresh(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_AUTO_REFRESH] = enabled }
    }

    suspend fun setTaskLogRefreshMs(ms: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_TASK_LOG_REFRESH] = ms }
        _taskLogRefreshMs.value = ms
    }

    suspend fun setSubLogRefreshMs(ms: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_SUB_LOG_REFRESH] = ms }
        _subLogRefreshMs.value = ms
    }

    suspend fun setScriptRunRefreshMs(ms: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_SCRIPT_RUN_REFRESH] = ms }
        _scriptRunRefreshMs.value = ms
    }

    suspend fun setConfirmBeforeRun(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_CONFIRM_BEFORE_RUN] = enabled }
    }

    suspend fun setShowSystemApps(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_SHOW_SYSTEM_APPS] = enabled }
    }

    suspend fun setLogCacheDays(days: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_LOG_CACHE_DAYS] = days }
    }

    suspend fun setTaskPageSize(size: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_TASK_PAGE_SIZE] = size }
    }

    suspend fun setSubPageSize(size: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_SUB_PAGE_SIZE] = size }
    }

    suspend fun setEnvPageSize(size: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_ENV_PAGE_SIZE] = size }
    }

    suspend fun clearAllData() {
        context.dataStore.edit { prefs -> prefs.clear() }
    }
}
