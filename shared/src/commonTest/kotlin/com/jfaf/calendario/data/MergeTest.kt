package com.jfaf.calendario.data

import com.jfaf.calendario.data.local.DayNoteEntity
import com.jfaf.calendario.data.local.LocalHolidayEntity
import com.jfaf.calendario.data.local.ShiftAssignmentEntity
import com.jfaf.calendario.data.local.ShiftEntity
import com.jfaf.calendario.data.remote.RemoteAssignment
import com.jfaf.calendario.data.remote.RemoteDayNote
import com.jfaf.calendario.data.remote.RemoteLocalHoliday
import com.jfaf.calendario.data.remote.RemoteShift
import com.jfaf.calendario.data.sync.isStale
import com.jfaf.calendario.data.sync.mergeAssignment
import com.jfaf.calendario.data.sync.mergeDayNote
import com.jfaf.calendario.data.sync.mergeHoliday
import com.jfaf.calendario.data.sync.mergeShift
import com.jfaf.calendario.data.sync.toRemote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Reglas de fusión: «el último cambio gana» por registro, sin perder cambios locales pendientes. */
class MergeTest {
    // --- turnos ---
    private fun localShift(updatedAt: Long, dirty: Boolean = true) = ShiftEntity(
        "s1", "Local", "L", 0xFF000000, 0, updatedAt, null, dirty, payMode = "PerHour", payCents = 1500,
    )

    private fun remoteShift(updatedAt: Long, deletedAt: Long? = null) = RemoteShift(
        "u1", "s1", "Remoto", "R", 0xFF111111, 2, 420, 900, 30, updatedAt, deletedAt, "0000000001",
    )

    @Test
    fun shiftWithoutLocalCopyIsTakenAsIs() {
        val merged = assertNotNull(mergeShift(null, remoteShift(10)))
        assertEquals("Remoto", merged.name)
        assertEquals(2, merged.sortOrder)
        assertEquals(420, merged.startMinutes)
        assertEquals(30, merged.breakMinutes)
        assertFalse(merged.dirty)
    }

    @Test
    fun newerRemoteShiftReplacesLocalAndClearsDirty() {
        val merged = assertNotNull(mergeShift(localShift(updatedAt = 5), remoteShift(10)))
        assertEquals("Remoto", merged.name)
        assertEquals(10, merged.updatedAt)
        assertFalse(merged.dirty)
    }

    @Test
    fun newerLocalShiftIsKeptUntouched() {
        assertNull(mergeShift(localShift(updatedAt = 20), remoteShift(10)))
    }

    @Test
    fun equalTimestampAcceptsRemoteSoThePushedRowStopsBeingDirty() {
        val merged = assertNotNull(mergeShift(localShift(updatedAt = 10, dirty = true), remoteShift(10)))
        assertFalse(merged.dirty)
    }

    @Test
    fun remoteDeletionOfAShiftPropagates() {
        val merged = assertNotNull(mergeShift(localShift(updatedAt = 5), remoteShift(10, deletedAt = 10)))
        assertEquals(10, merged.deletedAt)
    }

    @Test
    fun reservedPayColumnsAreKeptFromTheLocalRow() {
        val merged = assertNotNull(mergeShift(localShift(updatedAt = 5), remoteShift(10)))
        assertEquals("PerHour", merged.payMode)
        assertEquals(1500, merged.payCents)
    }

    // --- asignaciones ---
    private fun localAssignment(updatedAt: Long, shiftId: String = "default-night") =
        ShiftAssignmentEntity("2026-11-16#0", "2026-11-16", 0, shiftId, updatedAt, null, dirty = true)

    private fun remoteAssignment(updatedAt: Long, shiftId: String = "default-morning", deletedAt: Long? = null) =
        RemoteAssignment("u1", "2026-11-16#0", "2026-11-16", 0, shiftId, updatedAt, deletedAt, "0000000001")

    @Test
    fun assignmentRemoteWinsWhenNewerOrEqual() {
        assertEquals("default-morning", mergeAssignment(localAssignment(5), remoteAssignment(10))!!.shiftId)
        assertFalse(mergeAssignment(localAssignment(10), remoteAssignment(10))!!.dirty)
    }

    @Test
    fun assignmentLocalWinsWhenNewer() {
        assertNull(mergeAssignment(localAssignment(20), remoteAssignment(10)))
    }

    @Test
    fun assignmentDeletionPropagatesAndNewLocalRowIsCreated() {
        assertEquals(10, mergeAssignment(localAssignment(5), remoteAssignment(10, deletedAt = 10))!!.deletedAt)
        val created = mergeAssignment(null, remoteAssignment(10))!!
        assertEquals("2026-11-16#0", created.id)
        assertEquals(0, created.slot)
    }

    // --- festivos y notas ---
    @Test
    fun holidayFollowsTheSameRules() {
        val local = LocalHolidayEntity("2026-11-16", updatedAt = 5, deletedAt = null, dirty = true)
        val newer = RemoteLocalHoliday("u1", "2026-11-16", 10, deletedAt = 10, serverUpdatedAt = "1")
        val older = RemoteLocalHoliday("u1", "2026-11-16", 1, deletedAt = 1, serverUpdatedAt = "1")
        assertEquals(10, mergeHoliday(local, newer)!!.deletedAt)
        assertNull(mergeHoliday(local, older))
        assertTrue(mergeHoliday(null, newer)!!.dirty.not())
    }

    private fun localNote(updatedAt: Long) =
        DayNoteEntity("n1", "2026-11-16", "IOwe", "Ana", "local", settled = false, updatedAt, null, dirty = true)

    private fun remoteNote(updatedAt: Long, settled: Boolean = true) =
        RemoteDayNote("u1", "n1", "2026-11-16", "IOwe", "Ana", "remoto", settled, updatedAt, null, "1")

    @Test
    fun noteRemoteSettledStateWinsWhenNewer() {
        val merged = mergeDayNote(localNote(5), remoteNote(10))!!
        assertTrue(merged.settled)
        assertEquals("remoto", merged.body)
        assertFalse(merged.dirty)
    }

    @Test
    fun noteLocalEditIsKeptWhenNewer() {
        assertNull(mergeDayNote(localNote(20), remoteNote(10)))
    }

    @Test
    fun noteClearedPersonComesThroughAsNull() {
        val merged = mergeDayNote(localNote(5), remoteNote(10).copy(person = null))!!
        assertNull(merged.person)
    }

    // --- conversión a remoto ---
    @Test
    fun rowsAreSentWithTheOwnerAndAllTheirFields() {
        val shift = ShiftEntity("s1", "Mañana", "M", 0xFF2196F3, 1, 7, null, true, 360, 840, 20)
        assertEquals(
            RemoteShift("u9", "s1", "Mañana", "M", 0xFF2196F3, 1, 360, 840, 20, 7, null),
            shift.toRemote("u9"),
        )
        val note = localNote(3)
        assertEquals(RemoteDayNote("u9", "n1", "2026-11-16", "IOwe", "Ana", "local", false, 3, null), note.toRemote("u9"))
        assertEquals(RemoteAssignment("u9", "2026-11-16#0", "2026-11-16", 0, "default-night", 5, null), localAssignment(5).toRemote("u9"))
    }

    // --- cuándo sincronizar ---
    @Test
    fun stalenessRules() {
        assertTrue(isStale(null, 1_000, 30_000))
        assertFalse(isStale(1_000, 20_000, 30_000))
        assertTrue(isStale(1_000, 31_000, 30_000))
        assertTrue(isStale(50_000, 40_000, 30_000)) // el reloj retrocedió
    }
}
