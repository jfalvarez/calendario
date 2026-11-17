package com.jfaf.calendario.domain.repository

import com.jfaf.calendario.domain.model.ReminderSettings
import kotlinx.coroutines.flow.Flow

/** Ajustes de los avisos de turno (locales al dispositivo, no se sincronizan). */
interface ReminderSettingsRepository {
    fun observeReminderSettings(): Flow<ReminderSettings>
    suspend fun setReminderEnabled(enabled: Boolean)

    /** Hora del aviso en minutos desde medianoche (0..1439). */
    suspend fun setReminderTime(minutesOfDay: Int)
}
