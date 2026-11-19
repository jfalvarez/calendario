package com.jfaf.calendario.presentation

import com.jfaf.calendario.domain.model.DayNote
import com.jfaf.calendario.domain.model.NoteKind
import com.jfaf.calendario.domain.model.StatsPeriod
import com.jfaf.calendario.domain.repository.TodayProvider
import com.jfaf.calendario.domain.usecase.ObserveDebtsUseCase
import com.jfaf.calendario.domain.usecase.ObserveStatsUseCase
import com.jfaf.calendario.domain.usecase.SetNoteSettledUseCase
import com.jfaf.calendario.fakes.InMemoryDayNoteRepository
import com.jfaf.calendario.fakes.InMemoryShiftAssignmentRepository
import com.jfaf.calendario.fakes.InMemoryShiftRepository
import com.jfaf.calendario.presentation.debts.DebtsViewModel
import com.jfaf.calendario.presentation.stats.StatsViewModel
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    private val today = LocalDate(2026, 11, 19)
    private lateinit var assignments: InMemoryShiftAssignmentRepository
    private lateinit var vm: StatsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        assignments = InMemoryShiftAssignmentRepository()
        vm = StatsViewModel(
            observeStats = ObserveStatsUseCase(InMemoryShiftRepository(), assignments),
            todayProvider = TodayProvider { today },
        )
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsWithThisMonthAndCountsItsShifts() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.uiState.collect {} }
        assignments.setShiftIds(LocalDate(2026, 11, 3), listOf("default-morning"))
        assignments.setShiftIds(LocalDate(2026, 11, 4), listOf("default-morning", "default-night"))
        assignments.setShiftIds(LocalDate(2026, 10, 30), listOf("default-morning"))

        val state = vm.uiState.value
        assertEquals(StatsPeriod.ThisMonth, state.period)
        assertEquals(LocalDate(2026, 11, 1), state.from)
        assertEquals(LocalDate(2026, 11, 30), state.to)
        val stats = assertNotNull(state.stats)
        assertEquals(2, stats.daysWorked)
        assertEquals(3, stats.totalShifts)
    }

    @Test
    fun selectingLastMonthChangesTheRange() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.uiState.collect {} }
        assignments.setShiftIds(LocalDate(2026, 10, 30), listOf("default-morning"))
        vm.onPeriodSelected(StatsPeriod.LastMonth)
        val state = vm.uiState.value
        assertEquals(StatsPeriod.LastMonth, state.period)
        assertEquals(LocalDate(2026, 10, 1), state.from)
        assertEquals(LocalDate(2026, 10, 31), state.to)
        assertEquals(1, assertNotNull(state.stats).totalShifts)
    }

    @Test
    fun customRangeIsNormalizedWhenReversed() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onCustomRange(LocalDate(2026, 9, 15), LocalDate(2026, 9, 1))
        val state = vm.uiState.value
        assertEquals(StatsPeriod.Custom, state.period)
        assertEquals(LocalDate(2026, 9, 1), state.from)
        assertEquals(LocalDate(2026, 9, 15), state.to)
    }

    @Test
    fun customIsNotSelectableAsPreset() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onPeriodSelected(StatsPeriod.Custom)
        assertEquals(StatsPeriod.ThisMonth, vm.uiState.value.period)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DebtsViewModelTest {
    private lateinit var notes: InMemoryDayNoteRepository
    private lateinit var vm: DebtsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        notes = InMemoryDayNoteRepository(
            listOf(
                DayNote("d1", LocalDate(2026, 11, 2), NoteKind.IOwe, "Ana", "turno"),
                DayNote("d2", LocalDate(2026, 11, 5), NoteKind.IOwe, "Ana", "turno"),
                DayNote("d3", LocalDate(2026, 11, 6), NoteKind.OwedToMe, "Luis", "turno"),
            ),
        )
        vm = DebtsViewModel(ObserveDebtsUseCase(notes), SetNoteSettledUseCase(notes))
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun groupsDebtsByPersonAndStartsNull() = runTest(UnconfinedTestDispatcher()) {
        assertNull(vm.balances.value, "antes de leer los datos no se muestra 'sin deudas'")
        backgroundScope.launch { vm.balances.collect {} }
        val balances = assertNotNull(vm.balances.value)
        val ana = balances.single { it.person == "Ana" }
        assertEquals(2, ana.iOweCount)
        assertEquals(0, ana.owedToMeCount)
        assertEquals(1, balances.single { it.person == "Luis" }.owedToMeCount)
    }

    @Test
    fun settlingMovesDebtOutOfOpenList() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.balances.collect {} }
        vm.onSettle("d1", true)
        val ana = assertNotNull(vm.balances.value).single { it.person == "Ana" }
        assertEquals(1, ana.iOweCount)
        assertEquals(1, ana.settled.size)
    }
}
