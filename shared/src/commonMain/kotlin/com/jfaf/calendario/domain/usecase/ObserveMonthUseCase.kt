package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.CalendarDay
import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.Holiday
import com.jfaf.calendario.domain.repository.LocalHolidayRepository
import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import com.jfaf.calendario.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/** Semanas del mes con los turnos y festivos de cada día, actualizadas al cambiar turnos, asignaciones o festivos. */
class ObserveMonthUseCase(
    private val shifts: ShiftRepository,
    private val assignments: ShiftAssignmentRepository,
    private val localHolidays: LocalHolidayRepository,
) {
    operator fun invoke(
        month: CalendarMonth,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): Flow<List<List<CalendarDay>>> {
        val grid = buildMonthGrid(month, firstDayOfWeek)
        val from = grid.first().first()
        val to = grid.last().last()
        // La cuadrícula puede incluir días de otro año (enero/diciembre).
        val national = grid.flatten().map { it.year }.distinct()
            .flatMap { SpanishHolidays.nationalHolidays(it).entries }
            .associate { it.key to it.value }
        return combine(
            shifts.observeShifts(),
            assignments.observeAssignments(from, to),
            localHolidays.observeHolidays(from, to),
        ) { allShifts, assigned, local ->
            val byId = allShifts.associateBy { it.id }
            grid.map { week ->
                week.map { date ->
                    CalendarDay(
                        date = date,
                        inCurrentMonth = (date.month.ordinal + 1) == month.monthNumber,
                        shifts = assigned[date].orEmpty().mapNotNull { byId[it] },
                        holiday = holidayOf(date, national, local),
                    )
                }
            }
        }
    }
}

/** El festivo local manda sobre el nacional: es lo que el usuario ha marcado expresamente. */
private fun holidayOf(date: LocalDate, national: Map<LocalDate, String>, local: Set<LocalDate>): Holiday? = when {
    date in local -> Holiday(date, national[date], isLocal = true)
    else -> national[date]?.let { Holiday(date, it, isLocal = false) }
}
