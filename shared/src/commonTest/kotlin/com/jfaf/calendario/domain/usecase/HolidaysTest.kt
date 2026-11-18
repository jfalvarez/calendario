package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.fakes.InMemoryLocalHolidayRepository
import com.jfaf.calendario.fakes.InMemoryShiftAssignmentRepository
import com.jfaf.calendario.fakes.InMemoryShiftRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HolidaysTest {
    @Test
    fun goodFridayIsComputedFromEaster() {
        assertEquals(LocalDate(2026, 4, 3), SpanishHolidays.nationalHolidays(2026).entries.single { it.value == "Viernes Santo" }.key)
        assertEquals(LocalDate(2025, 4, 18), SpanishHolidays.nationalHolidays(2025).entries.single { it.value == "Viernes Santo" }.key)
        assertEquals(LocalDate(2024, 3, 29), SpanishHolidays.nationalHolidays(2024).entries.single { it.value == "Viernes Santo" }.key)
    }

    @Test
    fun yearHasTenNationalHolidays() {
        assertEquals(10, SpanishHolidays.nationalHolidays(2026).size)
    }

    @Test
    fun monthShowsNationalAndLocalHolidays() = runTest {
        val local = InMemoryLocalHolidayRepository()
        val observe = ObserveMonthUseCase(InMemoryShiftRepository(), InMemoryAssignments, local)
        ToggleLocalHolidaysUseCase(local)(setOf(LocalDate(2026, 11, 20)))
        val days = observe(CalendarMonth(2026, 11)).first().flatten()
        val national = days.single { it.date == LocalDate(2026, 11, 1) }.holiday
        assertNotNull(national)
        assertFalse(national.isLocal)
        assertTrue(days.single { it.date == LocalDate(2026, 11, 20) }.holiday!!.isLocal)
        assertNull(days.single { it.date == LocalDate(2026, 11, 21) }.holiday)
    }

    @Test
    fun togglingTwiceRemovesLocalHoliday() = runTest {
        val local = InMemoryLocalHolidayRepository()
        val toggle = ToggleLocalHolidaysUseCase(local)
        val day = setOf(LocalDate(2026, 11, 20))
        toggle(day)
        assertEquals(day, local.getHolidays(day))
        toggle(day)
        assertTrue(local.getHolidays(day).isEmpty())
    }

    private companion object {
        val InMemoryAssignments = InMemoryShiftAssignmentRepository()
    }
}
