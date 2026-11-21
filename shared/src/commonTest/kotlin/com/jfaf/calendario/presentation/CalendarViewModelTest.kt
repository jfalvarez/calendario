package com.jfaf.calendario.presentation

import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.platform.ForegroundNotifier
import com.jfaf.calendario.domain.repository.TodayProvider
import com.jfaf.calendario.domain.usecase.ObserveMonthUseCase
import com.jfaf.calendario.domain.usecase.ObserveNoteMarkersUseCase
import com.jfaf.calendario.domain.usecase.ObserveShiftsUseCase
import com.jfaf.calendario.domain.usecase.PaintDayUseCase
import com.jfaf.calendario.fakes.InMemoryDayNoteRepository
import com.jfaf.calendario.fakes.InMemoryLocalHolidayRepository
import com.jfaf.calendario.fakes.InMemoryShiftAssignmentRepository
import com.jfaf.calendario.fakes.InMemoryShiftRepository
import com.jfaf.calendario.presentation.calendar.CalendarDialog
import com.jfaf.calendario.presentation.calendar.CalendarMode
import com.jfaf.calendario.presentation.calendar.CalendarViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private var today = LocalDate(2026, 11, 19)
    private lateinit var assignments: InMemoryShiftAssignmentRepository
    private lateinit var shifts: InMemoryShiftRepository
    private lateinit var foreground: ForegroundNotifier
    private lateinit var vm: CalendarViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        shifts = InMemoryShiftRepository()
        assignments = InMemoryShiftAssignmentRepository()
        val holidays = InMemoryLocalHolidayRepository()
        val notes = InMemoryDayNoteRepository()
        foreground = ForegroundNotifier()
        vm = CalendarViewModel(
            observeMonth = ObserveMonthUseCase(shifts, assignments, holidays),
            observeShifts = ObserveShiftsUseCase(shifts),
            observeNoteMarkers = ObserveNoteMarkersUseCase(notes),
            paintDay = PaintDayUseCase(assignments, shifts),
            todayProvider = TodayProvider { today },
            foregroundEvents = foreground,
        )
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** La pantalla observa `uiState`; hay que suscribirse para que el flujo se active. */
    private fun TestScope.observe() {
        backgroundScope.launch { vm.uiState.collect {} }
    }

    private val d1 = LocalDate(2026, 11, 10)

    @Test
    fun paintingWithActiveShiftAssignsAndTappingAgainClears() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onShiftSelected("default-morning")
        vm.onPaintDay(d1)
        assertEquals(listOf("default-morning"), assignments.getShiftIds(d1))
        vm.onPaintDay(d1)
        assertEquals(emptyList(), assignments.getShiftIds(d1))
    }

    @Test
    fun paintingWithoutActiveShiftDoesNothing() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onPaintDay(d1)
        assertEquals(emptyList(), assignments.getShiftIds(d1))
    }

    @Test
    fun selectingTheActiveShiftAgainDeactivatesIt() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onShiftSelected("default-night")
        vm.onShiftSelected("default-night")
        assertEquals(null, vm.uiState.value.paintShiftId)
    }

    @Test
    fun modeChangeIsReflectedInState() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onModeChange(CalendarMode.Notes)
        assertEquals(CalendarMode.Notes, vm.uiState.value.mode)
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
    fun previousMonthMovesBackOneMonth() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onPreviousMonth()
        assertEquals(CalendarMonth(2026, 10), vm.uiState.value.month)
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

    @Test
    fun overlappingSecondShiftRaisesConflictUntilDismissed() = runTest(UnconfinedTestDispatcher()) {
        observe()
        shifts.upsert(Shift("early", "Mañana", "M", 0xFF2196F3, startMinutes = 6 * 60, endMinutes = 14 * 60))
        shifts.upsert(Shift("late", "Tarde", "T", 0xFFFF9800, startMinutes = 10 * 60, endMinutes = 18 * 60))
        assignments.setShiftIds(d1, listOf("early"))
        vm.onPaintSecondChange(true)
        vm.onShiftSelected("late")
        vm.onPaintDay(d1)
        assertEquals(CalendarDialog.ShiftConflict(listOf(d1)), vm.uiState.value.dialog)
        assertEquals(listOf("early"), assignments.getShiftIds(d1))
        vm.onDismissPaintConflict()
        assertNull(vm.uiState.value.dialog)
    }
}
