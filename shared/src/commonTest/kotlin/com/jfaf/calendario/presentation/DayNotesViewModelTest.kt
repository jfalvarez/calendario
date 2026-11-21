package com.jfaf.calendario.presentation

import com.jfaf.calendario.domain.model.DayNoteDraft
import com.jfaf.calendario.domain.model.NoteKind
import com.jfaf.calendario.domain.repository.IdGenerator
import com.jfaf.calendario.domain.usecase.DeleteDayNoteUseCase
import com.jfaf.calendario.domain.usecase.ObserveDayNotesUseCase
import com.jfaf.calendario.domain.usecase.ObserveDebtsUseCase
import com.jfaf.calendario.domain.usecase.SaveDayNoteUseCase
import com.jfaf.calendario.domain.usecase.SetNoteSettledUseCase
import com.jfaf.calendario.fakes.InMemoryDayNoteRepository
import com.jfaf.calendario.presentation.calendar.DayNotesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DayNotesViewModelTest {
    private val d1 = LocalDate(2026, 11, 10)
    private lateinit var notes: InMemoryDayNoteRepository
    private lateinit var vm: DayNotesViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        notes = InMemoryDayNoteRepository()
        vm = DayNotesViewModel(
            observeNotes = ObserveDayNotesUseCase(notes),
            observeDebts = ObserveDebtsUseCase(notes),
            saveNote = SaveDayNoteUseCase(notes, IdGenerator { "note-1" }),
            setNoteSettled = SetNoteSettledUseCase(notes),
            deleteNote = DeleteDayNoteUseCase(notes),
        )
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun dayClickOpensNotesForThatDayAndDismissClosesThem() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.uiState.collect {} }
        assertNull(vm.uiState.value)
        vm.onDayClick(d1)
        assertEquals(d1, vm.uiState.value?.date)
        vm.onDismissNotes()
        assertNull(vm.uiState.value)
    }

    @Test
    fun savingSettlingAndDeletingNoteUpdatesTheDayList() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onDayClick(d1)

        vm.onSaveNote(DayNoteDraft(id = null, date = d1, kind = NoteKind.IOwe, person = "Ana", text = ""))
        val saved = notes.all().single()
        assertEquals("note-1", saved.id)
        assertEquals("Ana", saved.person)
        assertEquals(listOf(saved), vm.uiState.value?.notes)
        assertEquals(listOf("Ana"), vm.uiState.value?.knownPeople)

        vm.onSettleNote(saved.id, true)
        assertTrue(notes.all().single().settled)

        vm.onDeleteNote(saved.id)
        assertTrue(notes.all().isEmpty())
    }

    @Test
    fun resetClosesTheDialog() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onDayClick(d1)
        vm.onReset()
        assertNull(vm.uiState.value)
    }
}
