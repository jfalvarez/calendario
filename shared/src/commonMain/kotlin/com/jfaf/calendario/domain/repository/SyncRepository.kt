package com.jfaf.calendario.domain.repository

import com.jfaf.calendario.domain.model.SyncState
import kotlinx.coroutines.flow.StateFlow

interface SyncRepository {
    val state: StateFlow<SyncState>
    /** Pide una sincronización cuanto antes (varias peticiones seguidas se funden en una). */
    fun requestSync()
}
