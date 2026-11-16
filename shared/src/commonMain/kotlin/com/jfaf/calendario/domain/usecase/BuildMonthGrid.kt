package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.CalendarMonth
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

/** Fechas del mes agrupadas en semanas completas (4 a 6 filas de 7 días). */
fun buildMonthGrid(month: CalendarMonth, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): List<List<LocalDate>> {
    val first = month.firstDay
    val offset = (first.dayOfWeek.isoDayNumber - firstDayOfWeek.isoDayNumber + 7) % 7
    val length = first.daysUntil(month.next.firstDay)
    val weeks = (offset + length + 6) / 7
    val start = first.plus(-offset, DateTimeUnit.DAY)
    return List(weeks) { w -> List(7) { d -> start.plus(w * 7 + d, DateTimeUnit.DAY) } }
}

private fun LocalDate.daysUntil(other: LocalDate): Int =
    (other.toEpochDays() - this.toEpochDays()).toInt()
