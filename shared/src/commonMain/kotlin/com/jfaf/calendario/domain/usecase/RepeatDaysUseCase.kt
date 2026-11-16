package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import kotlinx.datetime.LocalDate

/**
 * Repetir: toma el bloque seleccionado (del primer al último día) y lo repite `times` veces
 * a continuación. Con un límite de días para no escribir años enteros por error.
 */
class RepeatDaysUseCase(private val assignments: ShiftAssignmentRepository) {
    suspend operator fun invoke(dates: Set<LocalDate>, times: Int) {
        val source = assignments.patternOf(dates) ?: return
        val length = source.pattern.days.size
        val safeTimes = times.coerceIn(0, MAX_DAYS / length)
        val changes = buildMap {
            for (k in 1..safeTimes) putAll(source.pattern.changesFrom(source.start.plusDays(k * length)))
        }
        if (changes.isNotEmpty()) assignments.setAssignments(changes)
    }

    companion object {
        const val MAX_DAYS = 3660
    }
}
