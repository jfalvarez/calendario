package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import com.jfaf.calendario.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

/** Turnos con horario asignados a un día, en el orden de la app. Sin horario no hay aviso que dar. */
class GetShiftsForDayUseCase(
    private val assignments: ShiftAssignmentRepository,
    private val shifts: ShiftRepository,
) {
    /** Se lee en el momento de llamarlo: si el turno cambió entre tanto, lo que se devuelve es lo correcto. */
    suspend operator fun invoke(date: LocalDate): List<Shift> {
        val ids = assignments.getShiftIds(date)
        return shifts.observeShifts().first().filter { it.id in ids && it.startMinutes != null }
    }
}
