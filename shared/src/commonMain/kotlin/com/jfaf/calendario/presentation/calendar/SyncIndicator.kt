package com.jfaf.calendario.presentation.calendar

import com.jfaf.calendario.domain.model.SyncState

/** Lo que muestra el indicador del calendario. Sin cuenta no se muestra nada (`null`). */
enum class SyncIndicator { Pending, Syncing, Synced, Offline, Error }

fun SyncState.toIndicator(): SyncIndicator? = when (this) {
    SyncState.NotSignedIn -> null
    SyncState.Syncing -> SyncIndicator.Syncing
    is SyncState.Idle -> if (lastSyncedAt == null) SyncIndicator.Pending else SyncIndicator.Synced
    is SyncState.Failed -> if (offline) SyncIndicator.Offline else SyncIndicator.Error
}
