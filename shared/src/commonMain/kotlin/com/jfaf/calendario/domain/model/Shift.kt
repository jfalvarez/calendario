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

    /**
     * Franjas [inicio, fin) que ocupa el turno dentro de un mismo día. Un turno de noche se parte en medianoche.
     * Null si el turno no tiene horario.
     */
    private fun daySegments(): List<Pair<Int, Int>>? {
        val start = startMinutes ?: return null
        val end = endMinutes ?: return null
        val raw = ((end - start) % MINUTES_PER_DAY + MINUTES_PER_DAY) % MINUTES_PER_DAY
        val stop = start + (if (raw == 0) MINUTES_PER_DAY else raw)
        return if (stop <= MINUTES_PER_DAY) listOf(start to stop)
        else listOf(start to MINUTES_PER_DAY, 0 to stop - MINUTES_PER_DAY)
    }

    /** True si ambos turnos tienen horario y se solapan en alguna franja. Sin horario nunca hay solapamiento. */
    fun overlaps(other: Shift): Boolean {
        val mine = daySegments() ?: return false
        val theirs = other.daySegments() ?: return false
        return mine.any { (a, b) -> theirs.any { (c, d) -> a < d && c < b } }
    }

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
        const val MAX_ABBREVIATION_LENGTH = 3
    }
}
