package com.qinglong.app.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")

class ThemeManager(private val context: Context) {

    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val FOLLOW_SYSTEM_KEY = booleanPreferencesKey("follow_system")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FOLLOW_SYSTEM_KEY] ?: true
    }

    val followSystem: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FOLLOW_SYSTEM_KEY] ?: true
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
            preferences[FOLLOW_SYSTEM_KEY] = false
        }
    }

    suspend fun setFollowSystem(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FOLLOW_SYSTEM_KEY] = enabled
        }
    }
}
