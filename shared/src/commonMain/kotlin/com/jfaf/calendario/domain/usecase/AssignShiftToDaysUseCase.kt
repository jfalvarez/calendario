package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import kotlinx.datetime.LocalDate

/**
 * Asignar un turno a todos los días seleccionados. Por defecto sustituye lo que hubiera;
 * con `asSecond` lo añade como segundo turno (conserva el primero; máximo 2 por día).
 */
class AssignShiftToDaysUseCase(private val assignments: ShiftAssignmentRepository) {
    suspend operator fun invoke(dates: Set<LocalDate>, shiftId: String, asSecond: Boolean = false) {
        if (dates.isEmpty()) return
        val current = if (asSecond) assignments.getAssignments(dates.min(), dates.max()) else emptyMap()
        assignments.setAssignments(
            dates.associateWith { date ->
                val first = current[date].orEmpty().firstOrNull()
                if (asSecond && first != null && first != shiftId) listOf(first, shiftId) else listOf(shiftId)
            }
        )
    }
}
