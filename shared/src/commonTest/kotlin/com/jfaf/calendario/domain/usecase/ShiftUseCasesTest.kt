package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.model.ShiftValidationError
import com.jfaf.calendario.domain.repository.IdGenerator
import com.jfaf.calendario.fakes.InMemoryShiftRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShiftUseCasesTest {
    private val repo = InMemoryShiftRepository()
    private val save = SaveShiftUseCase(repo, IdGenerator { "new-id" })
    private fun shift(
        id: String = "", name: String = "Refuerzo", abbr: String = "R",
        start: Int? = null, end: Int? = null, brk: Int = 0,
    ) = Shift(id, name, abbr, 0xFF000000, start, end, brk)

    @Test
    fun blankIdGeneratesOneAndTrimsText() = runTest {
        assertNull(save(shift(name = "  Refuerzo  ", abbr = " R ")))
        val saved = repo.observeShifts().first().last()
        assertEquals("new-id", saved.id)
        assertEquals("Refuerzo", saved.name)
        assertEquals("R", saved.abbreviation)
    }

    @Test
    fun validationErrors() = runTest {
        assertEquals(ShiftValidationError.NameRequired, save(shift(name = " ")))
        assertEquals(ShiftValidationError.AbbreviationRequired, save(shift(abbr = "")))
        assertEquals(ShiftValidationError.AbbreviationTooLong, save(shift(abbr = "ABCD")))
        assertEquals(ShiftValidationError.TimesIncomplete, save(shift(start = 360)))
        assertEquals(ShiftValidationError.BreakInvalid, save(shift(start = 360, end = 420, brk = 60)))
        assertEquals(ShiftValidationError.BreakInvalid, save(shift(brk = -5)))
        assertEquals(3, repo.observeShifts().first().size) // nada se guardó
    }

    @Test
    fun workMinutesHandlesNightShiftAndBreak() {
        assertEquals(480, shift(start = 6 * 60, end = 14 * 60).workMinutes)
        assertEquals(450, shift(start = 22 * 60, end = 6 * 60, brk = 30).workMinutes)
        assertEquals(null, shift().workMinutes)
    }

    @Test
    fun deleteAndReorder() = runTest {
        DeleteShiftUseCase(repo)("default-afternoon")
        ReorderShiftsUseCase(repo)(listOf("default-night", "default-morning"))
        assertEquals(listOf("default-night", "default-morning"), repo.observeShifts().first().map { it.id })
    }
}
