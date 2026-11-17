package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.fakes.InMemoryLocalHolidayRepository
import com.jfaf.calendario.fakes.InMemoryShiftAssignmentRepository
import com.jfaf.calendario.fakes.InMemoryShiftRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ObserveUpcomingDaysUseCaseTest {
    private val shifts = InMemoryShiftRepository()
    private val assignments = InMemoryShiftAssignmentRepository()
    private val localHolidays = InMemoryLocalHolidayRepository()
    private val observe = ObserveUpcomingDaysUseCase(shifts, assignments, localHolidays)
    private val today = LocalDate(2025, 11, 12)

    @Test
    fun returnsTodayAndTomorrowWithTheirShifts() = runTest {
        assignments.setShiftIds(today, listOf("default-morning"))
        assignments.setShiftIds(LocalDate(2025, 11, 13), listOf("default-afternoon", "default-night"))

        val days = observe(today).first()

        assertEquals(listOf(today, LocalDate(2025, 11, 13)), days.map { it.date })
        assertEquals(listOf("M"), days[0].shifts.map { it.abbreviation })
        assertEquals(listOf("T", "N"), days[1].shifts.map { it.abbreviation })
    }

    @Test
    fun dayWithoutAssignmentHasNoShifts() = runTest {
        assertEquals(emptyList(), observe(today).first()[0].shifts)
    }

    @Test
    fun crossesMonthAndYearBoundary() = runTest {
        val days = observe(LocalDate(2025, 12, 31)).first()
        assertEquals(LocalDate(2026, 1, 1), days[1].date)
        assertNotNull(days[1].holiday) // Año Nuevo
    }

    @Test
    fun marksLocalHoliday() = runTest {
        localHolidays.setHoliday(setOf(LocalDate(2025, 11, 13)), true)
        val days = observe(today).first()
        assertNull(days[0].holiday)
        assertEquals(true, days[1].holiday?.isLocal)
    }

    @Test
    fun reflectsChangesWhileObserved() = runTest {
        assertEquals(emptyList(), observe(today).first()[0].shifts)
        assignments.setShiftIds(today, listOf("default-night"))
        assertEquals(listOf("N"), observe(today).first()[0].shifts.map { it.abbreviation })
    }
}
