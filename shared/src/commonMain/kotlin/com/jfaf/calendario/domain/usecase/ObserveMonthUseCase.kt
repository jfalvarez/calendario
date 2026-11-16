package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.CalendarDay
import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import com.jfaf.calendario.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DayOfWeek

/** Semanas del mes con los turnos de cada día, actualizadas al cambiar turnos o asignaciones. */
class ObserveMonthUseCase(
    private val shifts: ShiftRepository,
    private val assignments: ShiftAssignmentRepository,
) {
    operator fun invoke(
        month: CalendarMonth,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): Flow<List<List<CalendarDay>>> {
        val grid = buildMonthGrid(month, firstDayOfWeek)
        val from = grid.first().first()
        val to = grid.last().last()
        return combine(shifts.observeShifts(), assignments.observeAssignments(from, to)) { allShifts, assigned ->
            val byId = allShifts.associateBy { it.id }
            grid.map { week ->
                week.map { date ->
                    CalendarDay(
                        date = date,
                        inCurrentMonth = (date.month.ordinal + 1) == month.monthNumber,
                        shifts = assigned[date].orEmpty().mapNotNull { byId[it] },
                    )
                }
            }
        }
    }
}
