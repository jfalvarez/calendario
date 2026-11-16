package com.jfaf.calendario.domain.repository

import com.jfaf.calendario.domain.model.SyncState
import kotlinx.coroutines.flow.StateFlow

interface SyncRepository {
    val state: StateFlow<SyncState>
    /** Pide una sincronización cuanto antes (varias peticiones seguidas se funden en una). */
    fun requestSync()

    /**
     * Para cuando la app vuelve a primer plano: sincroniza sólo si la última sincronización correcta tiene
     * más de [STALE_AFTER_MS] (o no hubo ninguna), para no repetirla al cambiar de ventana a cada rato.
     */
    fun requestSyncIfStale()

    companion object {
        const val STALE_AFTER_MS = 30_000L
    }
}
