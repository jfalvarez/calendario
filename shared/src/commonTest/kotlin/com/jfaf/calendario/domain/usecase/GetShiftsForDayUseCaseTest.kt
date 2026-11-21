package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.fakes.InMemoryShiftAssignmentRepository
import com.jfaf.calendario.fakes.InMemoryShiftRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GetShiftsForDayUseCaseTest {
    private val day = LocalDate(2026, 11, 10)

    @Test
    fun returnsOnlyAssignedShiftsThatHaveHours() = runTest {
        val shifts = InMemoryShiftRepository()
        shifts.upsert(Shift("noche", "Noche", "N", 0xFF3F51B5, startMinutes = 22 * 60, endMinutes = 6 * 60))
        shifts.upsert(Shift("libre", "Libre", "L", 0xFF000000))
        val assignments = InMemoryShiftAssignmentRepository()
        assignments.setShiftIds(day, listOf("noche", "libre"))

        val result = GetShiftsForDayUseCase(assignments, shifts)(day)

        assertEquals(listOf("Noche"), result.map { it.name })
    }

    @Test
    fun emptyDayGivesNoShifts() = runTest {
        val result = GetShiftsForDayUseCase(InMemoryShiftAssignmentRepository(), InMemoryShiftRepository())(day)
        assertEquals(emptyList(), result)
    }
}
