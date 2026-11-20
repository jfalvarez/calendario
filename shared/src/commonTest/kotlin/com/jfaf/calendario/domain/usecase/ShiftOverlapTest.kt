package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.fakes.InMemoryShiftAssignmentRepository
import com.jfaf.calendario.fakes.InMemoryShiftRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShiftOverlapTest {
    private fun timed(id: String, start: Int, end: Int) = Shift(id, id, id.take(3), 0xFF000000, start, end)
    private fun h(hour: Int) = hour * 60

    @Test
    fun touchingEndpointsDoNotOverlap() {
        assertFalse(timed("m", h(6), h(14)).overlaps(timed("t", h(14), h(22))))
    }

    @Test
    fun partialAndContainedOverlapsAreDetected() {
        assertTrue(timed("a", h(8), h(14)).overlaps(timed("b", h(12), h(20))))
        assertTrue(timed("a", h(8), h(20)).overlaps(timed("b", h(10), h(12))))
    }

    @Test
    fun nightShiftWrapsMidnight() {
        val night = timed("n", h(22), h(6))
        assertTrue(night.overlaps(timed("early", h(5), h(7))))
        assertFalse(night.overlaps(timed("day", h(8), h(16))))
    }

    @Test
    fun shiftsWithoutHoursNeverOverlap() {
        assertFalse(Shift("libranza", "Libranza", "L", 0xFF000000).overlaps(timed("m", h(6), h(14))))
    }

    @Test
    fun conflictingSecondShiftWritesNothingAndReturnsDays() = runTest {
        val shifts = InMemoryShiftRepository()
        shifts.upsert(timed("a", h(8), h(14)))
        shifts.upsert(timed("b", h(12), h(20)))
        val repo = InMemoryShiftAssignmentRepository()
        val assign = AssignShiftToDaysUseCase(repo, shifts)
        val d1 = LocalDate(2025, 11, 1)
        val d2 = LocalDate(2025, 11, 2)

        assign(setOf(d1), "a")
        assign(setOf(d1), "b", asSecond = true).let { conflicts -> assertEquals(listOf(d1), conflicts) }
        assertEquals(listOf("a"), repo.getShiftIds(d1))

        // Sin choque en d2 se asigna; en d1 el choque bloquea toda la operación.
        val conflicts = assign(setOf(d1, d2), "b", asSecond = true)
        assertEquals(listOf(d1), conflicts)
        assertEquals(emptyList(), repo.getShiftIds(d2))
    }

    @Test
    fun nonOverlappingSecondShiftIsAssigned() = runTest {
        val shifts = InMemoryShiftRepository()
        shifts.upsert(timed("a", h(6), h(14)))
        shifts.upsert(timed("b", h(14), h(22)))
        val repo = InMemoryShiftAssignmentRepository()
        val assign = AssignShiftToDaysUseCase(repo, shifts)
        val d = LocalDate(2025, 11, 1)

        assign(setOf(d), "a")
        assertEquals(emptyList(), assign(setOf(d), "b", asSecond = true))
        assertEquals(listOf("a", "b"), repo.getShiftIds(d))
    }
}
