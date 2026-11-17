package com.jfaf.calendario.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jfaf.calendario.domain.model.AppStyle
import com.jfaf.calendario.domain.model.ThemeMode
import com.jfaf.calendario.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val styleKey = stringPreferencesKey("app_style")
    private val dismissedUpdateKey = intPreferencesKey("dismissed_recommended_version")

    override fun observeThemeMode(): Flow<ThemeMode> = store.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[themeKey] } ?: ThemeMode.System
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[themeKey] = mode.name }
    }

    override fun observeAppStyle(): Flow<AppStyle> = store.data.map { prefs ->
        AppStyle.entries.firstOrNull { it.name == prefs[styleKey] } ?: AppStyle.Pop
    }

    override suspend fun setAppStyle(style: AppStyle) {
        store.edit { it[styleKey] = style.name }
    }

    override fun observeDismissedRecommendedVersion(): Flow<Int> = store.data.map { it[dismissedUpdateKey] ?: 0 }

    override suspend fun setDismissedRecommendedVersion(versionCode: Int) {
        store.edit { it[dismissedUpdateKey] = versionCode }
    }
}
