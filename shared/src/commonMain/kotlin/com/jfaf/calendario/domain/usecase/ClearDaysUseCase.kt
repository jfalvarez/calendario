package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import kotlinx.datetime.LocalDate

/** Borrar: deja los días sin turnos. */
class ClearDaysUseCase(private val assignments: ShiftAssignmentRepository) {
    suspend operator fun invoke(dates: Set<LocalDate>) =
        assignments.setAssignments(dates.associateWith { emptyList() })
}
