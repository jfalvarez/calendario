package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.fakes.InMemoryShiftAssignmentRepository
import com.jfaf.calendario.fakes.InMemoryShiftRepository
import com.jfaf.calendario.domain.model.CalendarMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonthUseCasesTest {
    private val shifts = InMemoryShiftRepository()
    private val assignments = InMemoryShiftAssignmentRepository()
    private val paint = PaintDayUseCase(assignments)
    private val observeMonth = ObserveMonthUseCase(shifts, assignments)
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
    fun daysOutsideMonthAreMarked() = runTest {
        val cells = observeMonth(CalendarMonth(2025, 11)).first().flatten()
        assertEquals(false, cells.first().inCurrentMonth) // 27-oct
    }
}
