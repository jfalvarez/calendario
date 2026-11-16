package com.jfaf.calendario.domain.repository

import com.jfaf.calendario.domain.model.AppStyle
import com.jfaf.calendario.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Ajustes de la app (locales al dispositivo, no se sincronizan). */
interface SettingsRepository {
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    fun observeAppStyle(): Flow<AppStyle>
    suspend fun setAppStyle(style: AppStyle)
}
