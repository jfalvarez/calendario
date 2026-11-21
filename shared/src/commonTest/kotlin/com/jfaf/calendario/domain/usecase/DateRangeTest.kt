package com.jfaf.calendario.domain.usecase

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DateRangeTest {
    @Test
    fun includesBothEndsInAnyOrder() {
        val expected = (10..13).map { LocalDate(2026, 11, it) }.toSet()
        assertEquals(expected, dateRange(LocalDate(2026, 11, 10), LocalDate(2026, 11, 13)))
        assertEquals(expected, dateRange(LocalDate(2026, 11, 13), LocalDate(2026, 11, 10)))
    }

    @Test
    fun sameDayIsASingleDay() {
        val day = LocalDate(2026, 2, 28)
        assertEquals(setOf(day), dateRange(day, day))
    }

    @Test
    fun crossesMonthAndLeapYearBoundaries() {
        val range = dateRange(LocalDate(2028, 2, 28), LocalDate(2028, 3, 1))
        assertEquals(setOf(LocalDate(2028, 2, 28), LocalDate(2028, 2, 29), LocalDate(2028, 3, 1)), range)
    }
}
