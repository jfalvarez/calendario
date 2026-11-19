package com.jfaf.calendario.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.usecase.ObserveSyncStateUseCase
import com.jfaf.calendario.domain.usecase.RequestSyncUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Indicador de sincronización del calendario; tocarlo sincroniza ahora. */
class SyncStatusViewModel(
    observeSync: ObserveSyncStateUseCase,
    private val requestSync: RequestSyncUseCase,
) : ViewModel() {
    /** `null` si no hay cuenta: el indicador no se muestra. */
    val indicator: StateFlow<SyncIndicator?> = observeSync()
        .map { it.toIndicator() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onSyncClick() = requestSync()
}
