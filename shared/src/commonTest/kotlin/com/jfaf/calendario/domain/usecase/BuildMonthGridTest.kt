package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.CalendarMonth
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildMonthGridTest {
    @Test
    fun novemberStartsOnSaturdayAndNeedsFiveWeeks() {
        // 1-nov-2025 es sábado: con semana desde lunes, la primera fila empieza el 27-oct.
        val grid = buildMonthGrid(CalendarMonth(2025, 11))
        assertEquals(5, grid.size)
        assertEquals(LocalDate(2025, 10, 27), grid.first().first())
        assertEquals(LocalDate(2025, 11, 30), grid.last().last())
    }

    @Test
    fun februaryStartingOnMondayFitsFourWeeks() {
        // 1-feb-2027 es lunes y el mes tiene 28 días.
        assertEquals(4, buildMonthGrid(CalendarMonth(2027, 2)).size)
    }

    @Test
    fun sundayFirstShiftsTheGrid() {
        val grid = buildMonthGrid(CalendarMonth(2025, 11), DayOfWeek.SUNDAY)
        assertEquals(LocalDate(2025, 10, 26), grid.first().first())
    }

    @Test
    fun monthNavigationWrapsYear() {
        assertEquals(CalendarMonth(2026, 1), CalendarMonth(2025, 12).next)
        assertEquals(CalendarMonth(2024, 12), CalendarMonth(2025, 1).previous)
    }
}
