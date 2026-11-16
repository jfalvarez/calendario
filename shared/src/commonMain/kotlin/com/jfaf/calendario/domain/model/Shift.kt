package com.jfaf.calendario.domain.model

/**
 * Turno de trabajo configurable. Modelo de dominio puro (sin Android, Compose ni Room).
 * Las horas son minutos desde medianoche (0..1439); un turno de noche tiene `end < start`.
 */
data class Shift(
    val id: String,
    val name: String,
    val abbreviation: String,
    val colorArgb: Long,
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    val breakMinutes: Int = 0,
) {
    /** Minutos trabajados (descontando el descanso), o null si el turno no tiene horario. */
    val workMinutes: Int?
        get() {
            val start = startMinutes ?: return null
            val end = endMinutes ?: return null
            return (((end - start) % MINUTES_PER_DAY + MINUTES_PER_DAY) % MINUTES_PER_DAY - breakMinutes).coerceAtLeast(0)
        }

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
        const val MAX_ABBREVIATION_LENGTH = 3
    }
}
