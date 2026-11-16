package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.AppStyle
import com.jfaf.calendario.domain.model.ThemeMode
import com.jfaf.calendario.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeUseCasesTest {
    private val repo = object : SettingsRepository {
        val mode = MutableStateFlow(ThemeMode.System)
        val style = MutableStateFlow(AppStyle.Pop)
        override fun observeThemeMode(): Flow<ThemeMode> = mode
        override suspend fun setThemeMode(mode: ThemeMode) { this.mode.value = mode }
        override fun observeAppStyle(): Flow<AppStyle> = style
        override suspend fun setAppStyle(style: AppStyle) { this.style.value = style }
    }

    @Test
    fun setThenObserve() = runTest {
        assertEquals(ThemeMode.System, ObserveThemeModeUseCase(repo)().first())
        SetThemeModeUseCase(repo)(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, ObserveThemeModeUseCase(repo)().first())
    }

    @Test
    fun styleDefaultsToPopAndCanChange() = runTest {
        assertEquals(AppStyle.Pop, ObserveAppStyleUseCase(repo)().first())
        SetAppStyleUseCase(repo)(AppStyle.Neon)
        assertEquals(AppStyle.Neon, ObserveAppStyleUseCase(repo)().first())
    }
}
