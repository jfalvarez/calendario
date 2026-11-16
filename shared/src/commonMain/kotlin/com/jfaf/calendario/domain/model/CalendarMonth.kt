package com.jfaf.calendario.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** Mes de calendario (`monthNumber` 1..12). */
data class CalendarMonth(val year: Int, val monthNumber: Int) {
    val firstDay: LocalDate get() = LocalDate(year, monthNumber, 1)
    val next: CalendarMonth get() = of(firstDay.plus(1, DateTimeUnit.MONTH))
    val previous: CalendarMonth get() = of(firstDay.plus(-1, DateTimeUnit.MONTH))

    companion object {
        fun of(date: LocalDate) = CalendarMonth(date.year, (date.month.ordinal + 1))
    }
}
