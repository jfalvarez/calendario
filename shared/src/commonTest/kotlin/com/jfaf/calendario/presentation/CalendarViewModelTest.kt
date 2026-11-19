package com.jfaf.calendario.presentation

import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.DayNoteDraft
import com.jfaf.calendario.domain.model.NoteKind
import com.jfaf.calendario.domain.model.SyncState
import com.jfaf.calendario.domain.repository.IdGenerator
import com.jfaf.calendario.domain.repository.TodayProvider
import com.jfaf.calendario.domain.usecase.AssignShiftToDaysUseCase
import com.jfaf.calendario.domain.usecase.ClearDaysUseCase
import com.jfaf.calendario.domain.usecase.CopyDaysUseCase
import com.jfaf.calendario.domain.usecase.DeleteDayNoteUseCase
import com.jfaf.calendario.domain.usecase.ObserveDayNotesUseCase
import com.jfaf.calendario.domain.usecase.ObserveDebtsUseCase
import com.jfaf.calendario.domain.usecase.ObserveMonthUseCase
import com.jfaf.calendario.domain.usecase.ObserveShiftsUseCase
import com.jfaf.calendario.domain.usecase.ObserveSyncStateUseCase
import com.jfaf.calendario.domain.usecase.PaintDayUseCase
import com.jfaf.calendario.domain.usecase.PasteDaysUseCase
import com.jfaf.calendario.domain.usecase.RepeatDaysUseCase
import com.jfaf.calendario.domain.usecase.RequestSyncUseCase
import com.jfaf.calendario.domain.usecase.SaveDayNoteUseCase
import com.jfaf.calendario.domain.usecase.SetNoteSettledUseCase
import com.jfaf.calendario.domain.usecase.ToggleLocalHolidaysUseCase
import com.jfaf.calendario.fakes.FakeSyncRepository
import com.jfaf.calendario.fakes.InMemoryDayNoteRepository
import com.jfaf.calendario.fakes.InMemoryLocalHolidayRepository
import com.jfaf.calendario.fakes.InMemoryShiftAssignmentRepository
import com.jfaf.calendario.fakes.InMemoryShiftRepository
import com.jfaf.calendario.presentation.calendar.CalendarMode
import com.jfaf.calendario.presentation.calendar.CalendarViewModel
import com.jfaf.calendario.presentation.update.AppForegroundEvents
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
class CalendarViewModelTest {
    private var today = LocalDate(2026, 11, 19)
    private lateinit var shifts: InMemoryShiftRepository
    private lateinit var assignments: InMemoryShiftAssignmentRepository
    private lateinit var holidays: InMemoryLocalHolidayRepository
    private lateinit var notes: InMemoryDayNoteRepository
    private lateinit var sync: FakeSyncRepository
    private lateinit var foreground: AppForegroundEvents
    private lateinit var vm: CalendarViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        shifts = InMemoryShiftRepository()
        assignments = InMemoryShiftAssignmentRepository()
        holidays = InMemoryLocalHolidayRepository()
        notes = InMemoryDayNoteRepository()
        sync = FakeSyncRepository()
        foreground = AppForegroundEvents()
        vm = CalendarViewModel(
            observeMonth = ObserveMonthUseCase(shifts, assignments, holidays),
            observeShifts = ObserveShiftsUseCase(shifts),
            paintDay = PaintDayUseCase(assignments),
            copyDays = CopyDaysUseCase(assignments),
            pasteDays = PasteDaysUseCase(assignments),
            clearDays = ClearDaysUseCase(assignments),
            assignShift = AssignShiftToDaysUseCase(assignments),
            repeatDays = RepeatDaysUseCase(assignments),
            toggleLocalHolidays = ToggleLocalHolidaysUseCase(holidays),
            observeNotes = ObserveDayNotesUseCase(notes),
            observeDebts = ObserveDebtsUseCase(notes),
            saveNote = SaveDayNoteUseCase(notes, IdGenerator { "note-1" }),
            setNoteSettled = SetNoteSettledUseCase(notes),
            deleteNote = DeleteDayNoteUseCase(notes),
            observeSync = ObserveSyncStateUseCase(sync),
            requestSync = RequestSyncUseCase(sync),
            todayProvider = TodayProvider { today },
            foregroundEvents = foreground,
        )
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** La pantalla observa `uiState`; hay que suscribirse para que el flujo se active. */
    private fun kotlinx.coroutines.test.TestScope.observe() {
        backgroundScope.launch { vm.uiState.collect {} }
    }

    private val d1 = LocalDate(2026, 11, 10)
    private val d2 = LocalDate(2026, 11, 11)

    @Test
    fun paintingWithActiveShiftAssignsAndTappingAgainClears() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onShiftSelected("default-morning")
        vm.onDayClick(d1)
        assertEquals(listOf("default-morning"), assignments.getShiftIds(d1))
        vm.onDayClick(d1)
        assertEquals(emptyList(), assignments.getShiftIds(d1))
    }

    @Test
    fun paintingWithoutActiveShiftDoesNothing() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onDayClick(d1)
        assertEquals(emptyList(), assignments.getShiftIds(d1))
    }

    @Test
    fun tappingSelectsAndDeselectsDaysInEditMode() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onModeChange(CalendarMode.Edit)
        vm.onDayClick(d1)
        vm.onDayClick(d2)
        assertEquals(setOf(d1, d2), vm.uiState.value.selection)
        vm.onDayClick(d1)
        assertEquals(setOf(d2), vm.uiState.value.selection)
    }

    @Test
    fun longPressSelectsRangeForwardAndBackward() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onModeChange(CalendarMode.Edit)
        vm.onDayClick(LocalDate(2026, 11, 10))
        vm.onDayLongPress(LocalDate(2026, 11, 13))
        assertEquals((10..13).map { LocalDate(2026, 11, it) }.toSet(), vm.uiState.value.selection)

        vm.onClearSelection()
        vm.onDayClick(LocalDate(2026, 11, 20))
        vm.onDayLongPress(LocalDate(2026, 11, 17))
        assertEquals((17..20).map { LocalDate(2026, 11, it) }.toSet(), vm.uiState.value.selection)
    }

    @Test
    fun longPressOutsideEditModeIsIgnored() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onDayLongPress(d1)
        assertTrue(vm.uiState.value.selection.isEmpty())
    }

    @Test
    fun changingModeClearsSelectionAndDialog() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onModeChange(CalendarMode.Edit)
        vm.onDayClick(d1)
        vm.onAssignClick()
        assertTrue(vm.uiState.value.dialog != null)
        vm.onModeChange(CalendarMode.Paint)
        assertTrue(vm.uiState.value.selection.isEmpty())
        assertNull(vm.uiState.value.dialog)
    }

    @Test
    fun copyThenPasteRepeatsTheBlockFromTheFirstSelectedDay() = runTest(UnconfinedTestDispatcher()) {
        observe()
        assignments.setShiftIds(d1, listOf("default-night"))
        vm.onModeChange(CalendarMode.Edit)
        vm.onDayClick(d1)
        vm.onCopy()
        assertTrue(vm.uiState.value.hasClipboard)

        vm.onClearSelection()
        vm.onDayClick(LocalDate(2026, 11, 20))
        vm.onPaste()
        assertEquals(listOf("default-night"), assignments.getShiftIds(LocalDate(2026, 11, 20)))
    }

    @Test
    fun cutCopiesAndClearsTheSelection() = runTest(UnconfinedTestDispatcher()) {
        observe()
        assignments.setShiftIds(d1, listOf("default-afternoon"))
        vm.onModeChange(CalendarMode.Edit)
        vm.onDayClick(d1)
        vm.onCut()
        assertTrue(vm.uiState.value.hasClipboard)
        assertEquals(emptyList(), assignments.getShiftIds(d1))
    }

    @Test
    fun deleteClearsSelectedDays() = runTest(UnconfinedTestDispatcher()) {
        observe()
        assignments.setShiftIds(d1, listOf("default-afternoon"))
        vm.onModeChange(CalendarMode.Edit)
        vm.onDayClick(d1)
        vm.onDelete()
        assertEquals(emptyList(), assignments.getShiftIds(d1))
    }

    @Test
    fun togglingLocalHolidayMarksThenUnmarksSelection() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onModeChange(CalendarMode.Edit)
        vm.onDayClick(d1)
        vm.onToggleLocalHoliday()
        assertEquals(setOf(d1), holidays.getHolidays(setOf(d1)))
        vm.onToggleLocalHoliday()
        assertEquals(emptySet(), holidays.getHolidays(setOf(d1)))
    }

    @Test
    fun notesModeOpensDayAndSavesNote() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onModeChange(CalendarMode.Notes)
        vm.onDayClick(d1)
        assertEquals(d1, vm.uiState.value.notesDialog?.date)

        vm.onSaveNote(DayNoteDraft(id = null, date = d1, kind = NoteKind.IOwe, person = "Ana", text = ""))
        val saved = notes.all().single()
        assertEquals("note-1", saved.id)
        assertEquals("Ana", saved.person)
        assertEquals(listOf(saved), vm.uiState.value.notesDialog?.notes)

        vm.onSettleNote(saved.id, true)
        assertTrue(notes.all().single().settled)
        vm.onDeleteNote(saved.id)
        assertTrue(notes.all().isEmpty())
    }

    @Test
    fun syncIndicatorFollowsSyncState() = runTest(UnconfinedTestDispatcher()) {
        observe()
        assertNull(vm.uiState.value.syncIndicator, "sin cuenta no hay indicador")
        sync.setState(SyncState.Syncing)
        assertEquals(com.jfaf.calendario.presentation.calendar.SyncIndicator.Syncing, vm.uiState.value.syncIndicator)
        vm.onSyncClick()
        assertEquals(1, sync.syncRequests)
    }

    @Test
    fun todayButtonReturnsToCurrentMonth() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onNextMonth()
        assertEquals(CalendarMonth(2026, 12), vm.uiState.value.month)
        vm.onToday()
        assertEquals(CalendarMonth(2026, 11), vm.uiState.value.month)
    }

    @Test
    fun returningToForegroundRecalculatesTodayAndMonth() = runTest(UnconfinedTestDispatcher()) {
        observe()
        assertEquals(CalendarMonth(2026, 11), vm.uiState.value.month)
        today = LocalDate(2026, 12, 2)
        foreground.notifyForeground()
        assertEquals(LocalDate(2026, 12, 2), vm.uiState.value.today)
        assertEquals(CalendarMonth(2026, 12), vm.uiState.value.month)
    }
}
