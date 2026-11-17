package com.jfaf.calendario.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/** Aviso del turno del día siguiente: si está activo y a qué hora del día (minutos desde medianoche) se emite. */
data class ReminderSettings(
    val enabled: Boolean = false,
    val minutesOfDay: Int = DEFAULT_MINUTES_OF_DAY,
) {
    companion object {
        const val DEFAULT_MINUTES_OF_DAY = 21 * 60
    }
}

/** Próximo aviso a emitir: [triggerAt] es la hora local del aviso y [shiftDate] el día (posterior) del turno. */
data class NextReminder(val shiftDate: LocalDate, val triggerAt: LocalDateTime)
