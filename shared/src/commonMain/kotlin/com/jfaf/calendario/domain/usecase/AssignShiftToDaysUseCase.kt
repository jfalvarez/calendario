package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import com.jfaf.calendario.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

/**
 * Asignar un turno a todos los días seleccionados. Por defecto sustituye lo que hubiera;
 * con `asSecond` lo añade como segundo turno (conserva el primero; máximo 2 por día).
 * Un segundo turno no puede solaparse en horas con los que ya tiene el día.
 */
class AssignShiftToDaysUseCase(
    private val assignments: ShiftAssignmentRepository,
    private val shifts: ShiftRepository,
) {
    /**
     * Devuelve los días donde el turno chocaría con otro ya asignado. Si hay alguno no se escribe nada;
     * una lista vacía significa que la asignación se ha hecho.
     */
    suspend operator fun invoke(dates: Set<LocalDate>, shiftId: String, asSecond: Boolean = false): List<LocalDate> {
        if (dates.isEmpty()) return emptyList()
        if (!asSecond) {
            assignments.setAssignments(dates.associateWith { listOf(shiftId) })
            return emptyList()
        }
        val current = assignments.getAssignments(dates.min(), dates.max())
        val byId = shifts.observeShifts().first().associateBy { it.id }
        val incoming = byId[shiftId]
        val conflicts = dates.filter { date ->
            incoming != null && current[date].orEmpty()
                .filter { it != shiftId }
                .mapNotNull { byId[it] }
                .any { incoming.overlaps(it) }
        }.sorted()
        if (conflicts.isNotEmpty()) return conflicts
        assignments.setAssignments(
            dates.associateWith { date ->
                val first = current[date].orEmpty().firstOrNull()
                if (first != null && first != shiftId) listOf(first, shiftId) else listOf(shiftId)
            }
        )
        return emptyList()
    }
}
