package com.jfaf.calendario.presentation

import com.jfaf.calendario.domain.usecase.AssignShiftToDaysUseCase
import com.jfaf.calendario.domain.usecase.ClearDaysUseCase
import com.jfaf.calendario.domain.usecase.CopyDaysUseCase
import com.jfaf.calendario.domain.usecase.PasteDaysUseCase
import com.jfaf.calendario.domain.usecase.RepeatDaysUseCase
import com.jfaf.calendario.domain.usecase.ToggleLocalHolidaysUseCase
import com.jfaf.calendario.fakes.InMemoryLocalHolidayRepository
import com.jfaf.calendario.fakes.InMemoryShiftAssignmentRepository
import com.jfaf.calendario.presentation.calendar.CalendarDialog
import com.jfaf.calendario.presentation.calendar.DayEditingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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
class DayEditingViewModelTest {
    private lateinit var assignments: InMemoryShiftAssignmentRepository
    private lateinit var holidays: InMemoryLocalHolidayRepository
    private lateinit var vm: DayEditingViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        assignments = InMemoryShiftAssignmentRepository()
        holidays = InMemoryLocalHolidayRepository()
        vm = DayEditingViewModel(
            copyDays = CopyDaysUseCase(assignments),
            pasteDays = PasteDaysUseCase(assignments),
            clearDays = ClearDaysUseCase(assignments),
            assignShift = AssignShiftToDaysUseCase(assignments),
            repeatDays = RepeatDaysUseCase(assignments),
            toggleLocalHolidays = ToggleLocalHolidaysUseCase(holidays),
        )
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.observe() {
        backgroundScope.launch { vm.uiState.collect {} }
    }

    private val d1 = LocalDate(2026, 11, 10)
    private val d2 = LocalDate(2026, 11, 11)

    private fun days(from: Int, to: Int) = (from..to).map { LocalDate(2026, 11, it) }.toSet()

    @Test
    fun tappingSelectsAndDeselectsDays() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onDayToggle(d1)
        vm.onDayToggle(d2)
        assertEquals(setOf(d1, d2), vm.uiState.value.selection)
        vm.onDayToggle(d1)
        assertEquals(setOf(d2), vm.uiState.value.selection)
    }

    @Test
    fun longPressSelectsRangeForwardAndBackward() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onDayToggle(LocalDate(2026, 11, 10))
        vm.onDayLongPress(LocalDate(2026, 11, 13))
        assertEquals(days(10, 13), vm.uiState.value.selection)

        vm.onClearSelection()
        vm.onDayToggle(LocalDate(2026, 11, 20))
        vm.onDayLongPress(LocalDate(2026, 11, 17))
        assertEquals(days(17, 20), vm.uiState.value.selection)
    }

    @Test
    fun longPressWithoutAnchorSelectsOnlyThatDay() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onDayLongPress(d1)
        assertEquals(setOf(d1), vm.uiState.value.selection)
    }

    @Test
    fun copyThenPasteRepeatsTheBlockFromTheFirstSelectedDay() = runTest(UnconfinedTestDispatcher()) {
        observe()
        assignments.setShiftIds(d1, listOf("default-night"))
        vm.onDayToggle(d1)
        vm.onCopy()
        assertTrue(vm.uiState.value.hasClipboard)

        vm.onClearSelection()
        vm.onDayToggle(LocalDate(2026, 11, 20))
        vm.onPaste()
        assertEquals(listOf("default-night"), assignments.getShiftIds(LocalDate(2026, 11, 20)))
    }

    @Test
    fun cutCopiesAndClearsTheSelection() = runTest(UnconfinedTestDispatcher()) {
        observe()
        assignments.setShiftIds(d1, listOf("default-afternoon"))
        vm.onDayToggle(d1)
        vm.onCut()
        assertTrue(vm.uiState.value.hasClipboard)
        assertEquals(emptyList(), assignments.getShiftIds(d1))
    }

    @Test
    fun deleteClearsSelectedDays() = runTest(UnconfinedTestDispatcher()) {
        observe()
        assignments.setShiftIds(d1, listOf("default-afternoon"))
        vm.onDayToggle(d1)
        vm.onDelete()
        assertEquals(emptyList(), assignments.getShiftIds(d1))
    }

    @Test
    fun togglingLocalHolidayMarksThenUnmarksSelection() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onDayToggle(d1)
        vm.onToggleLocalHoliday()
        assertEquals(setOf(d1), holidays.getHolidays(setOf(d1)))
        vm.onToggleLocalHoliday()
        assertEquals(emptySet(), holidays.getHolidays(setOf(d1)))
    }

    @Test
    fun assignDialogOpensAndAssigningClosesIt() = runTest(UnconfinedTestDispatcher()) {
        observe()
        vm.onDayToggle(d1)
        vm.onAssignClick()
        assertEquals(CalendarDialog.Assign, vm.uiState.value.dialog)
        vm.onAssign("default-morning", asSecond = false)
        assertNull(vm.uiState.value.dialog)
        assertEquals(listOf("default-morning"), assignments.getShiftIds(d1))
    }

    @Test
    fun repeatDialogRepeatsTheSelectedPattern() = runTest(UnconfinedTestDispatcher()) {
        observe()
        assignments.setShiftIds(d1, listOf("default-morning"))
        vm.onDayToggle(d1)
        vm.onRepeatClick()
        assertEquals(CalendarDialog.Repeat, vm.uiState.value.dialog)
        vm.onRepeat(2)
        assertNull(vm.uiState.value.dialog)
        assertEquals(listOf("default-morning"), assignments.getShiftIds(LocalDate(2026, 11, 12)))
    }

    @Test
    fun resetForgetsSelectionAndDialogButKeepsClipboard() = runTest(UnconfinedTestDispatcher()) {
        observe()
        assignments.setShiftIds(d1, listOf("default-morning"))
        vm.onDayToggle(d1)
        vm.onCopy()
        vm.onAssignClick()
        vm.onReset()
        assertTrue(vm.uiState.value.selection.isEmpty())
        assertNull(vm.uiState.value.dialog)
        assertTrue(vm.uiState.value.hasClipboard)
    }

    @Test
    fun actionsWithoutSelectionDoNothing() = runTest(UnconfinedTestDispatcher()) {
        observe()
        assignments.setShiftIds(d1, listOf("default-morning"))
        vm.onDelete()
        vm.onAssignClick()
        assertEquals(listOf("default-morning"), assignments.getShiftIds(d1))
        assertNull(vm.uiState.value.dialog)
    }
}
