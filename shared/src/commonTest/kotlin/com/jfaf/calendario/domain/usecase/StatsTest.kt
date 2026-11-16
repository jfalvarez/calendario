package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.model.StatsPeriod
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsTest {
    private val morning = Shift("m", "Mañana", "M", 0, 6 * 60, 14 * 60) // 8 h
    private val night = Shift("n", "Noche", "N", 0, 22 * 60, 6 * 60, 30) // 7,5 h
    private fun d(m: Int, day: Int) = LocalDate(2025, m, day)

    @Test
    fun aggregatesRangeIgnoringOutsideAndDeletedShifts() {
        val assigned = mapOf(
            d(11, 1) to listOf("m"),
            d(11, 2) to listOf("m", "n"),        // dos turnos el mismo día: 1 día, 2 turnos
            d(11, 3) to listOf("gone"),          // turno eliminado: se ignora
            d(12, 1) to listOf("m"),             // fuera del rango
        )
        val stats = computeStats(listOf(morning, night), assigned, d(11, 1), d(11, 30))
        assertEquals(2, stats.daysWorked)
        assertEquals(3, stats.totalShifts)
        assertEquals(listOf("m" to 2, "n" to 1), stats.perShift.map { it.shift.id to it.count })
        assertEquals(2 * 480 + 450, stats.totalMinutes)
    }

    @Test
    fun emptyRange() {
        val stats = computeStats(listOf(morning), emptyMap(), d(11, 1), d(11, 30))
        assertEquals(0, stats.daysWorked)
        assertEquals(0, stats.totalShifts)
        assertEquals(emptyList(), stats.perShift)
        assertEquals(0, stats.totalMinutes)
    }

    @Test
    fun periodsResolveAgainstToday() {
        val today = LocalDate(2026, 3, 15)
        assertEquals(LocalDate(2026, 3, 1)..LocalDate(2026, 3, 31), StatsPeriod.ThisMonth.resolve(today))
        assertEquals(LocalDate(2026, 2, 1)..LocalDate(2026, 2, 28), StatsPeriod.LastMonth.resolve(today))
        assertEquals(LocalDate(2026, 1, 1)..LocalDate(2026, 12, 31), StatsPeriod.ThisYear.resolve(today))
        assertEquals(LocalDate(2026, 2, 14)..today, StatsPeriod.Last30Days.resolve(today))
        assertEquals(null, StatsPeriod.Custom.resolve(today))
        assertEquals(LocalDate(2025, 12, 1)..LocalDate(2025, 12, 31), StatsPeriod.LastMonth.resolve(LocalDate(2026, 1, 5)))
    }
}
