package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.DayPattern
import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import kotlinx.datetime.LocalDate

/** Pegar: escribe el bloque copiado empezando en `start`, sustituyendo lo que hubiera. */
class PasteDaysUseCase(private val assignments: ShiftAssignmentRepository) {
    suspend operator fun invoke(pattern: DayPattern, start: LocalDate) =
        assignments.setAssignments(pattern.changesFrom(start))
}
