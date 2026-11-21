package com.jfaf.calendario.presentation

import com.jfaf.calendario.domain.model.SyncState
import com.jfaf.calendario.domain.usecase.ObserveSyncStateUseCase
import com.jfaf.calendario.domain.usecase.RequestSyncUseCase
import com.jfaf.calendario.fakes.FakeSyncRepository
import com.jfaf.calendario.presentation.calendar.SyncIndicator
import com.jfaf.calendario.presentation.calendar.SyncStatusViewModel
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SyncStatusViewModelTest {
    private lateinit var sync: FakeSyncRepository
    private lateinit var vm: SyncStatusViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        sync = FakeSyncRepository()
        vm = SyncStatusViewModel(ObserveSyncStateUseCase(sync), RequestSyncUseCase(sync))
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun followsSyncStateAndHidesWithoutAccount() = runTest(UnconfinedTestDispatcher()) {
        backgroundScope.launch { vm.indicator.collect {} }
        assertNull(vm.indicator.value)
        sync.setState(SyncState.Syncing)
        assertEquals(SyncIndicator.Syncing, vm.indicator.value)
        sync.setState(SyncState.Failed(offline = true))
        assertEquals(SyncIndicator.Offline, vm.indicator.value)
        sync.setState(SyncState.Idle(lastSyncedAt = 1L))
        assertEquals(SyncIndicator.Synced, vm.indicator.value)
    }

    @Test
    fun tappingRequestsSync() = runTest(UnconfinedTestDispatcher()) {
        vm.onSyncClick()
        assertEquals(1, sync.syncRequests)
    }
}
