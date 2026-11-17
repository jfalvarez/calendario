package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.CalendarDay
import com.jfaf.calendario.domain.repository.LocalHolidayRepository
import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import com.jfaf.calendario.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Los [days] días consecutivos a partir de [from] (hoy, mañana…) con sus turnos y festivo, actualizados al cambiar
 * turnos, asignaciones o festivos. Lo usa el widget; `inCurrentMonth` es siempre `true` (no hay cuadrícula de mes).
 */
class ObserveUpcomingDaysUseCase(
    private val shifts: ShiftRepository,
    private val assignments: ShiftAssignmentRepository,
    private val localHolidays: LocalHolidayRepository,
) {
    operator fun invoke(from: LocalDate, days: Int = 2): Flow<List<CalendarDay>> {
        require(days > 0) { "days debe ser positivo" }
        val dates = (0 until days).map { from.plus(DatePeriod(days = it)) }
        val to = dates.last()
        val national = dates.map { it.year }.distinct()
            .flatMap { SpanishHolidays.nationalHolidays(it).entries }
            .associate { it.key to it.value }
        return combine(
            shifts.observeShifts(),
            assignments.observeAssignments(from, to),
            localHolidays.observeHolidays(from, to),
        ) { allShifts, assigned, local ->
            val byId = allShifts.associateBy { it.id }
            dates.map { date ->
                CalendarDay(
                    date = date,
                    inCurrentMonth = true,
                    shifts = assigned[date].orEmpty().mapNotNull { byId[it] },
                    holiday = holidayOf(date, national, local),
                )
            }
        }
    }
}
