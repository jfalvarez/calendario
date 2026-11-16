package com.jfaf.calendario.presentation

import com.jfaf.calendario.domain.repository.IdGenerator
import com.jfaf.calendario.domain.usecase.DeleteShiftUseCase
import com.jfaf.calendario.domain.usecase.ObserveShiftsUseCase
import com.jfaf.calendario.domain.usecase.ReorderShiftsUseCase
import com.jfaf.calendario.domain.usecase.SaveShiftUseCase
import com.jfaf.calendario.fakes.InMemoryShiftRepository
import com.jfaf.calendario.presentation.shifts.ShiftEditorError
import com.jfaf.calendario.presentation.shifts.ShiftsViewModel
import com.jfaf.calendario.presentation.shifts.parseMinutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ShiftsViewModelTest {
    private val repo = InMemoryShiftRepository()
    private lateinit var vm: ShiftsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = ShiftsViewModel(
            ObserveShiftsUseCase(repo),
            SaveShiftUseCase(repo, IdGenerator { "new-id" }),
            DeleteShiftUseCase(repo),
            ReorderShiftsUseCase(repo),
        )
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun parsesTimes() {
        assertEquals(390, parseMinutes("06:30"))
        assertEquals(390, parseMinutes("6:30"))
        assertNull(parseMinutes("25:00"))
        assertNull(parseMinutes("0630"))
        assertNull(parseMinutes(""))
    }

    @Test
    fun createsShiftFromEditor() = runTest {
        vm.uiState.onEach { }.launchIn(kotlinx.coroutines.CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.onAdd()
        vm.onEditorChange { it.copy(name = "Refuerzo", abbreviation = "R", start = "08:00", end = "12:30", breakMinutes = "15") }
        vm.onSave()
        assertNull(vm.uiState.value.editor)
        val saved = repo.observeShifts().first().last()
        assertEquals("new-id", saved.id)
        assertEquals(8 * 60, saved.startMinutes)
        assertEquals(255, saved.workMinutes)
    }

    @Test
    fun invalidTimeKeepsEditorOpenWithError() = runTest {
        vm.uiState.onEach { }.launchIn(kotlinx.coroutines.CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.onAdd()
        vm.onEditorChange { it.copy(name = "X", abbreviation = "X", start = "99:00", end = "10:00") }
        vm.onSave()
        assertEquals(ShiftEditorError.TimeFormat, vm.uiState.value.editor?.error)
    }

    @Test
    fun movesShiftDown() = runTest {
        vm.uiState.onEach { }.launchIn(kotlinx.coroutines.CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.onMove("default-morning", 1)
        assertEquals(
            listOf("default-afternoon", "default-morning", "default-night"),
            repo.observeShifts().first().map { it.id },
        )
    }

}
