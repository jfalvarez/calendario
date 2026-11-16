package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.DayPattern
import com.jfaf.calendario.fakes.InMemoryShiftAssignmentRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class EditDaysUseCasesTest {
    private val repo = InMemoryShiftAssignmentRepository()
    private val copy = CopyDaysUseCase(repo)
    private val paste = PasteDaysUseCase(repo)
    private val clear = ClearDaysUseCase(repo)
    private val assign = AssignShiftToDaysUseCase(repo)
    private val repeat = RepeatDaysUseCase(repo)

    private fun d(day: Int) = LocalDate(2025, 11, day)
    private suspend fun ids(day: Int) = repo.getShiftIds(d(day))

    @Test
    fun assignReplacesAndAsSecondKeepsFirst() = runTest {
        assign(setOf(d(1), d(2)), "M")
        assertEquals(listOf("M"), ids(1))
        assign(setOf(d(1), d(3)), "N", asSecond = true)
        assertEquals(listOf("M", "N"), ids(1))
        assertEquals(listOf("N"), ids(3)) // día vacío: queda como primer turno
        assign(setOf(d(1)), "T", asSecond = true)
        assertEquals(listOf("M", "T"), ids(1)) // sustituye sólo el segundo
        assign(setOf(d(1)), "T")
        assertEquals(listOf("T"), ids(1)) // sin asSecond: sustituye todo
    }

    @Test
    fun copyIsSparseAndPasteSkipsUnselectedDays() = runTest {
        assign(setOf(d(1)), "M"); assign(setOf(d(2)), "T"); assign(setOf(d(3)), "N")
        val pattern = copy(setOf(d(1), d(3)))!!
        assertEquals(DayPattern(listOf(listOf("M"), null, listOf("N"))), pattern)
        paste(pattern, d(10))
        assertEquals(listOf("M"), ids(10))
        assertEquals(emptyList(), ids(11))
        assertEquals(listOf("N"), ids(12))
    }

    @Test
    fun pasteOverwritesEvenWithEmptyDays() = runTest {
        assign(setOf(d(5)), "T")
        paste(DayPattern(listOf(emptyList())), d(5))
        assertEquals(emptyList(), ids(5))
    }

    @Test
    fun clearRemovesShifts() = runTest {
        assign(setOf(d(1), d(2)), "M")
        clear(setOf(d(1), d(2)))
        assertEquals(emptyList(), ids(1))
        assertEquals(emptyList(), ids(2))
    }

    @Test
    fun repeatContinuesTheBlock() = runTest {
        assign(setOf(d(1)), "M"); assign(setOf(d(2)), "T")
        repeat(setOf(d(1), d(2)), times = 2)
        assertEquals(listOf("M", "T", "M", "T", "M", "T"), (1..6).map { ids(it).single() })
        assertEquals(emptyList(), ids(7))
    }

    @Test
    fun repeatWithNothingSelectedOrZeroTimesDoesNothing() = runTest {
        assign(setOf(d(1)), "M")
        repeat(emptySet(), 3)
        repeat(setOf(d(1)), 0)
        assertEquals(emptyList(), ids(2))
    }
}
