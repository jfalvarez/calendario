package com.jfaf.calendario.presentation

import com.jfaf.calendario.domain.model.AuthState
import com.jfaf.calendario.domain.model.LinkRequestResult
import com.jfaf.calendario.domain.model.PendingRequest
import com.jfaf.calendario.domain.repository.SocialError
import com.jfaf.calendario.domain.usecase.ObserveAuthStateUseCase
import com.jfaf.calendario.fakes.FakeAuthRepository
import com.jfaf.calendario.fakes.FakeSocialRepository
import com.jfaf.calendario.presentation.social.SocialViewModel
import com.jfaf.calendario.presentation.update.AppForegroundEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SocialViewModelTest {
    private lateinit var social: FakeSocialRepository
    private lateinit var auth: FakeAuthRepository
    private lateinit var foreground: AppForegroundEvents

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        social = FakeSocialRepository()
        auth = FakeAuthRepository(AuthState.SignedIn("user-1", "ana@correo.es"))
        foreground = AppForegroundEvents()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun newVm() = SocialViewModel(social, ObserveAuthStateUseCase(auth), foreground)

    @Test
    fun loadsOverviewWhenSignedIn() = runTest(UnconfinedTestDispatcher()) {
        social.alias = "ana_pepe_77"
        social.pending = listOf(PendingRequest("l1", "pepe"))
        val vm = newVm()
        backgroundScope.launch { vm.uiState.collect {} }
        assertEquals("ana_pepe_77", vm.uiState.value.myAlias)
        assertEquals(listOf(PendingRequest("l1", "pepe")), vm.uiState.value.pending)
    }

    @Test
    fun invalidAliasNeverReachesTheServer() = runTest(UnconfinedTestDispatcher()) {
        val vm = newVm()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.saveAlias("a b")
        assertTrue(vm.uiState.value.invalidAliasInput)
        assertFalse(vm.uiState.value.aliasSaved)
        assertTrue(social.calls.none { it.startsWith("setAlias") })
    }

    @Test
    fun validAliasIsSavedAndShown() = runTest(UnconfinedTestDispatcher()) {
        val vm = newVm()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.saveAlias("  ana_pepe_77  ")
        val state = vm.uiState.value
        assertTrue(state.aliasSaved)
        assertFalse(state.invalidAliasInput)
        assertEquals("ana_pepe_77", state.myAlias)
        assertTrue(social.calls.contains("setAlias:ana_pepe_77"))
    }

    @Test
    fun requestLinkShowsServerResult() = runTest(UnconfinedTestDispatcher()) {
        social.linkRequestResult = LinkRequestResult.AliasNotFound
        val vm = newVm()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.requestLink("nadie_99")
        assertEquals(LinkRequestResult.AliasNotFound, vm.uiState.value.requestResult)
    }

    @Test
    fun serverErrorIsShownAndListsAreNotReloaded() = runTest(UnconfinedTestDispatcher()) {
        val vm = newVm()
        backgroundScope.launch { vm.uiState.collect {} }
        social.error = SocialError.Network
        social.pending = listOf(PendingRequest("l1", "pepe"))
        vm.accept("l1")
        assertEquals(SocialError.Network, vm.uiState.value.error)
        assertFalse(vm.uiState.value.busy)
        assertTrue(vm.uiState.value.pending.isEmpty(), "con error no se recargan las listas")
    }

    @Test
    fun successfulActionReloadsLists() = runTest(UnconfinedTestDispatcher()) {
        val vm = newVm()
        backgroundScope.launch { vm.uiState.collect {} }
        social.pending = listOf(PendingRequest("l1", "pepe"))
        vm.accept("l1")
        assertTrue(social.calls.contains("accept:l1"))
        assertNull(vm.uiState.value.error)
        assertEquals(listOf(PendingRequest("l1", "pepe")), vm.uiState.value.pending)
    }

    @Test
    fun aliasTakenErrorIsMappedToItsDomainError() = runTest(UnconfinedTestDispatcher()) {
        val vm = newVm()
        backgroundScope.launch { vm.uiState.collect {} }
        social.error = SocialError.AliasTaken
        vm.saveAlias("ana_pepe_77")
        assertEquals(SocialError.AliasTaken, vm.uiState.value.error)
        assertFalse(vm.uiState.value.aliasSaved)
    }

    @Test
    fun clearMessagesResetsFeedback() = runTest(UnconfinedTestDispatcher()) {
        val vm = newVm()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.saveAlias("a b")
        vm.clearMessages()
        assertFalse(vm.uiState.value.invalidAliasInput)
        assertNull(vm.uiState.value.error)
    }
}
