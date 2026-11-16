package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.DayPattern
import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import kotlinx.datetime.LocalDate

/** Copiar: devuelve el bloque de días seleccionados (null si no hay selección). */
class CopyDaysUseCase(private val assignments: ShiftAssignmentRepository) {
    suspend operator fun invoke(dates: Set<LocalDate>): DayPattern? = assignments.patternOf(dates)?.pattern
}
