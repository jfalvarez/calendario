package com.jfaf.calendario.presentation

import com.jfaf.calendario.domain.model.SharedAssignment
import com.jfaf.calendario.domain.repository.TodayProvider
import com.jfaf.calendario.domain.usecase.BuildSwapOptionsUseCase
import com.jfaf.calendario.domain.usecase.LoadSharedScheduleUseCase
import com.jfaf.calendario.domain.usecase.ProposeSwapUseCase
import com.jfaf.calendario.domain.repository.SocialError
import com.jfaf.calendario.fakes.FakeSocialRepository
import com.jfaf.calendario.fakes.InMemoryShiftAssignmentRepository
import com.jfaf.calendario.fakes.InMemoryShiftRepository
import com.jfaf.calendario.domain.model.MyShiftOption
import com.jfaf.calendario.presentation.social.SharedScheduleViewModel
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SharedScheduleViewModelTest {
    private val today = LocalDate(2026, 11, 19)
    private lateinit var social: FakeSocialRepository
    private lateinit var assignments: InMemoryShiftAssignmentRepository
    private lateinit var shifts: InMemoryShiftRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        social = FakeSocialRepository()
        assignments = InMemoryShiftAssignmentRepository()
        shifts = InMemoryShiftRepository()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun newVm() = SharedScheduleViewModel(
        ownerId = "owner-1",
        alias = "pepe",
        loadSchedule = LoadSharedScheduleUseCase(social),
        proposeSwap = ProposeSwapUseCase(social),
        buildSwapOptions = BuildSwapOptionsUseCase(shifts, assignments),
        today = TodayProvider { today },
    )

    private val target = SharedAssignment(
        date = LocalDate(2026, 11, 25), slot = 0, shiftName = "Tarde", abbreviation = "T",
        colorArgb = 0xFFFF9800, startMinutes = null, endMinutes = null, breakMinutes = 0,
    )

    @Test
    fun loadsCurrentMonthOfTheCompanion() = runTest(UnconfinedTestDispatcher()) {
        social.schedule = listOf(target)
        val vm = newVm()
        backgroundScope.launch { vm.uiState.collect {} }
        val state = vm.uiState.value
        assertFalse(state.loading)
        assertNull(state.error)
        assertEquals(listOf(target), state.assignments)
        assertTrue(social.calls.contains("schedule:owner-1:2026-11"))
    }

    @Test
    fun loadErrorIsShownAndRetryReloads() = runTest(UnconfinedTestDispatcher()) {
        social.error = SocialError.Network
        val vm = newVm()
        backgroundScope.launch { vm.uiState.collect {} }
        assertEquals(SocialError.Network, vm.uiState.value.error)
        assertFalse(vm.uiState.value.loading)

        social.error = null
        social.schedule = listOf(target)
        vm.retry()
        assertNull(vm.uiState.value.error)
        assertEquals(listOf(target), vm.uiState.value.assignments)
    }

    @Test
    fun changingMonthReloadsThatMonth() = runTest(UnconfinedTestDispatcher()) {
        val vm = newVm()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onNextMonth()
        assertEquals(2026, vm.uiState.value.month.year)
        assertEquals(12, vm.uiState.value.month.monthNumber)
        assertTrue(social.calls.contains("schedule:owner-1:2026-12"))
    }

    @Test
    fun proposalOffersOnlyMyShiftsInNext60DaysWithNames() = runTest(UnconfinedTestDispatcher()) {
        assignments.setShiftIds(LocalDate(2026, 11, 18), listOf("default-night"))
        assignments.setShiftIds(LocalDate(2026, 11, 21), listOf("default-morning"))
        assignments.setShiftIds(LocalDate(2026, 12, 1), listOf("default-afternoon", "default-night"))
        assignments.setShiftIds(LocalDate(2027, 2, 1), listOf("default-morning"))
        val vm = newVm()
        backgroundScope.launch { vm.uiState.collect {} }

        vm.openProposal(target)

        val options = vm.uiState.value.proposal?.options
        assertNotNull(options)
        assertEquals(
            listOf(
                MyShiftOption(LocalDate(2026, 11, 21), 0, "Mañana"),
                MyShiftOption(LocalDate(2026, 12, 1), 0, "Tarde"),
                MyShiftOption(LocalDate(2026, 12, 1), 1, "Noche"),
            ),
            options,
        )
    }

    @Test
    fun sendingProposalClosesItOnSuccess() = runTest(UnconfinedTestDispatcher()) {
        assignments.setShiftIds(LocalDate(2026, 11, 21), listOf("default-morning"))
        val vm = newVm()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.openProposal(target)
        val option = vm.uiState.value.proposal!!.options.single()
        vm.selectOption(option)
        assertEquals(2, vm.uiState.value.proposal?.preview?.size, "mi día entregado y el del compañero")

        vm.sendProposal()

        assertNull(vm.uiState.value.proposal)
        assertTrue(social.calls.contains("propose:owner-1:2026-11-21:0:2026-11-25:0"))
    }

    @Test
    fun sendingProposalKeepsItOpenWithErrorOnFailure() = runTest(UnconfinedTestDispatcher()) {
        assignments.setShiftIds(LocalDate(2026, 11, 21), listOf("default-morning"))
        val vm = newVm()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.openProposal(target)
        vm.selectOption(vm.uiState.value.proposal!!.options.single())

        social.error = SocialError.TargetBusy
        vm.sendProposal()

        val draft = vm.uiState.value.proposal
        assertNotNull(draft)
        assertEquals(SocialError.TargetBusy, draft.error)
        assertFalse(draft.sending)
    }

    @Test
    fun openingAgainKeepsTheOpenProposalAndItsSelection() = runTest(UnconfinedTestDispatcher()) {
        assignments.setShiftIds(LocalDate(2026, 11, 21), listOf("default-morning"))
        val vm = newVm()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.openProposal(target)
        val option = vm.uiState.value.proposal!!.options.single()
        vm.selectOption(option)
        vm.openProposal(target)
        assertEquals(option, vm.uiState.value.proposal?.selected)
    }

    @Test
    fun sendWithoutSelectionDoesNothing() = runTest(UnconfinedTestDispatcher()) {
        val vm = newVm()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.openProposal(target)
        vm.sendProposal()
        assertTrue(social.calls.none { it.startsWith("propose") })
        assertNotNull(vm.uiState.value.proposal)
    }
}
