package com.jfaf.calendario.domain.usecase

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
        override fun observeThemeMode(): Flow<ThemeMode> = mode
        override suspend fun setThemeMode(mode: ThemeMode) { this.mode.value = mode }
    }

    @Test
    fun setThenObserve() = runTest {
        assertEquals(ThemeMode.System, ObserveThemeModeUseCase(repo)().first())
        SetThemeModeUseCase(repo)(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, ObserveThemeModeUseCase(repo)().first())
    }
}
