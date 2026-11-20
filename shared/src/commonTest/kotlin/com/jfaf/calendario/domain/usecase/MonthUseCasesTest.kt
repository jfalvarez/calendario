package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.fakes.InMemoryShiftAssignmentRepository
import com.jfaf.calendario.fakes.InMemoryShiftRepository
import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.Shift
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonthUseCasesTest {
    private val shifts = InMemoryShiftRepository()
    private val assignments = InMemoryShiftAssignmentRepository()
    private val localHolidays = com.jfaf.calendario.fakes.InMemoryLocalHolidayRepository()
    private val paint = PaintDayUseCase(assignments, shifts)
    private val observeMonth = ObserveMonthUseCase(shifts, assignments, localHolidays)
    private val day = LocalDate(2025, 11, 12)

    @Test
    fun paintedShiftAppearsOnThatDay() = runTest {
        paint(day, "default-morning")
        val cell = observeMonth(CalendarMonth(2025, 11)).first().flatten().single { it.date == day }
        assertEquals(listOf("M"), cell.shifts.map { it.abbreviation })
        assertTrue(cell.inCurrentMonth)
    }

    @Test
    fun paintingSameShiftTwiceClearsTheDay() = runTest {
        paint(day, "default-morning")
        paint(day, "default-morning")
        assertEquals(emptyList(), assignments.getShiftIds(day))
    }

    @Test
    fun paintingOtherShiftReplaces() = runTest {
        paint(day, "default-morning")
        paint(day, "default-afternoon")
        assertEquals(listOf("default-afternoon"), assignments.getShiftIds(day))
    }

    @Test
    fun paintingAsSecondAddsAndTogglesOff() = runTest {
        paint(day, "default-morning")
        assertEquals(false, paint(day, "default-afternoon", asSecond = true))
        assertEquals(listOf("default-morning", "default-afternoon"), assignments.getShiftIds(day))
        // Pintar el mismo turno como segundo lo quita.
        paint(day, "default-afternoon", asSecond = true)
        assertEquals(listOf("default-morning"), assignments.getShiftIds(day))
    }

    @Test
    fun secondPaintRefusedWhenOverlappingOrFull() = runTest {
        shifts.upsert(Shift("early", "Temprano", "E", 0xFF000000, 6 * 60, 14 * 60))
        shifts.upsert(Shift("late", "Tarde", "L", 0xFF000000, 12 * 60, 20 * 60))
        paint(day, "early")
        assertEquals(true, paint(day, "late", asSecond = true))
        assertEquals(listOf("early"), assignments.getShiftIds(day))

        shifts.upsert(Shift("noon", "Mediodía", "D", 0xFF000000, 14 * 60, 16 * 60))
        paint(day, "default-night", asSecond = true)
        assertEquals(true, paint(day, "noon", asSecond = true))
        assertEquals(listOf("early", "default-night"), assignments.getShiftIds(day))
    }

    @Test
    fun daysOutsideMonthAreMarked() = runTest {
        val cells = observeMonth(CalendarMonth(2025, 11)).first().flatten()
        assertEquals(false, cells.first().inCurrentMonth) // 27-oct
    }
}
