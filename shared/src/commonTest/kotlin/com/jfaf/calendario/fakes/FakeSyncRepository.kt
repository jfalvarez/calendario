package com.jfaf.calendario.fakes

import com.jfaf.calendario.domain.model.SyncState
import com.jfaf.calendario.domain.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Falso de [SyncRepository]: cuenta las peticiones de sincronización. */
class FakeSyncRepository(initial: SyncState = SyncState.NotSignedIn) : SyncRepository {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<SyncState> = _state.asStateFlow()

    var syncRequests = 0
        private set
    var staleRequests = 0
        private set

    fun setState(value: SyncState) { _state.value = value }

    override fun requestSync() { syncRequests++ }
    override fun requestSyncIfStale() { staleRequests++ }
}
