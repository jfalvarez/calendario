package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.AppStyle
import com.jfaf.calendario.domain.model.ThemeMode
import com.jfaf.calendario.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveThemeModeUseCase(private val settings: SettingsRepository) {
    operator fun invoke(): Flow<ThemeMode> = settings.observeThemeMode()
}

class SetThemeModeUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(mode: ThemeMode) = settings.setThemeMode(mode)
}

class ObserveAppStyleUseCase(private val settings: SettingsRepository) {
    operator fun invoke(): Flow<AppStyle> = settings.observeAppStyle()
}

class SetAppStyleUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(style: AppStyle) = settings.setAppStyle(style)
}
