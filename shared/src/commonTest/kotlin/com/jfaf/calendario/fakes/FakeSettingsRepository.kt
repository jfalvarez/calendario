package com.jfaf.calendario.fakes

import com.jfaf.calendario.domain.model.AppStyle
import com.jfaf.calendario.domain.model.ThemeMode
import com.jfaf.calendario.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow

/** Falso de [SettingsRepository] en memoria. */
class FakeSettingsRepository(
    theme: ThemeMode = ThemeMode.System,
    style: AppStyle = AppStyle.Pop,
) : SettingsRepository {
    private val theme = MutableStateFlow(theme)
    private val style = MutableStateFlow(style)
    private val dismissed = MutableStateFlow(0)

    override fun observeThemeMode(): Flow<ThemeMode> = theme
    override suspend fun setThemeMode(mode: ThemeMode) { theme.value = mode }
    override fun observeAppStyle(): Flow<AppStyle> = style
    override suspend fun setAppStyle(style: AppStyle) { this.style.value = style }
    override fun observeDismissedRecommendedVersion(): Flow<Int> = dismissed
    override suspend fun setDismissedRecommendedVersion(versionCode: Int) { dismissed.value = versionCode }
}
