package com.jfaf.calendario.presentation

import com.jfaf.calendario.domain.model.SyncState
import com.jfaf.calendario.presentation.calendar.SyncIndicator
import com.jfaf.calendario.presentation.calendar.toIndicator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SyncIndicatorTest {
    @Test
    fun mapsEverySyncState() {
        assertNull(SyncState.NotSignedIn.toIndicator())
        assertEquals(SyncIndicator.Syncing, SyncState.Syncing.toIndicator())
        assertEquals(SyncIndicator.Pending, SyncState.Idle(null).toIndicator())
        assertEquals(SyncIndicator.Synced, SyncState.Idle(123L).toIndicator())
        assertEquals(SyncIndicator.Offline, SyncState.Failed(offline = true).toIndicator())
        assertEquals(SyncIndicator.Error, SyncState.Failed(offline = false).toIndicator())
    }
}
