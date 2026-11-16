package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import kotlinx.datetime.LocalDate

/** Modo «Pintar»: asigna el turno al día; si el día ya tenía sólo ese turno, lo borra. */
class PaintDayUseCase(private val assignments: ShiftAssignmentRepository) {
    suspend operator fun invoke(date: LocalDate, shiftId: String) {
        val current = assignments.getShiftIds(date)
        assignments.setShiftIds(date, if (current == listOf(shiftId)) emptyList() else listOf(shiftId))
    }
}
