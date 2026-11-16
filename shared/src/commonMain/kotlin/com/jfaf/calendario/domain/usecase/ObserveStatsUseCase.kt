package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.PeriodStats
import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.model.ShiftStats
import com.jfaf.calendario.domain.model.StatsPeriod
import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import com.jfaf.calendario.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** Turnos, horas y días del periodo, actualizados al cambiar turnos o asignaciones. */
class ObserveStatsUseCase(
    private val shifts: ShiftRepository,
    private val assignments: ShiftAssignmentRepository,
) {
    operator fun invoke(from: LocalDate, to: LocalDate): Flow<PeriodStats> =
        combine(shifts.observeShifts(), assignments.observeAssignments(from, to)) { allShifts, assigned ->
            computeStats(allShifts, assigned, from, to)
        }
}

/** Cálculo puro. Las asignaciones a turnos eliminados se ignoran. */
fun computeStats(
    shifts: List<Shift>,
    assignments: Map<LocalDate, List<String>>,
    from: LocalDate,
    to: LocalDate,
): PeriodStats {
    val byId = shifts.associateBy { it.id }
    val counts = mutableMapOf<String, Int>()
    var daysWorked = 0
    for ((date, ids) in assignments) {
        if (date < from || date > to) continue
        val known = ids.filter { it in byId }
        if (known.isNotEmpty()) daysWorked++
        known.forEach { counts[it] = (counts[it] ?: 0) + 1 }
    }
    val perShift = shifts.mapNotNull { shift ->
        val count = counts[shift.id] ?: return@mapNotNull null
        ShiftStats(shift, count, count * (shift.workMinutes ?: 0))
    }
    return PeriodStats(
        from = from,
        to = to,
        perShift = perShift,
        daysWorked = daysWorked,
        totalShifts = perShift.sumOf { it.count },
        totalMinutes = perShift.sumOf { it.minutes },
    )
}

/** Rango del periodo predefinido respecto a `today`; null para `Custom`. */
fun StatsPeriod.resolve(today: LocalDate): ClosedRange<LocalDate>? = when (this) {
    StatsPeriod.ThisMonth -> CalendarMonth.of(today).let { it.firstDay..it.next.firstDay.plus(-1, DateTimeUnit.DAY) }
    StatsPeriod.LastMonth -> CalendarMonth.of(today).previous.let { it.firstDay..it.next.firstDay.plus(-1, DateTimeUnit.DAY) }
    StatsPeriod.ThisYear -> LocalDate(today.year, 1, 1)..LocalDate(today.year, 12, 31)
    StatsPeriod.Last30Days -> today.plus(-29, DateTimeUnit.DAY)..today
    StatsPeriod.Custom -> null
}
