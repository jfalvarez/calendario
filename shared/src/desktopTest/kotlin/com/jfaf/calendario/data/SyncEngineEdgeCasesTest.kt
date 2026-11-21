@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.jfaf.calendario.data

import com.jfaf.calendario.domain.model.DayNote
import com.jfaf.calendario.domain.model.NoteKind
import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.model.SyncState
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
/** Casos límite de la sincronización: carreras a mitad de pasada, fallos parciales, conflictos y cuentas. */
class SyncEngineEdgeCasesTest {
    private val remote = FakeRemote()
    private val a = Device(remote)
    private val b = Device(remote)
    private val day = LocalDate(2026, 11, 16)
    private val otherDay = LocalDate(2026, 11, 17)

    @AfterTest
    fun tearDown() { a.close(); b.close() }

    private suspend fun Device.ids(d: LocalDate = day) = assignments.getShiftIds(d)
    private fun pause() = Thread.sleep(15) // los cambios se ordenan por reloj (ms)
    private suspend fun Device.dirtyRows() =
        db.shiftDao().getDirty().size + db.shiftAssignmentDao().getDirty().size +
            db.localHolidayDao().getDirty().size + db.dayNoteDao().getDirty().size

    @Test
    fun anEditMadeWhileSyncingIsNotLostNorMarkedAsSent() = runTest {
        a.assignments.setShiftIds(day, listOf("default-morning"))
        // A mitad de la pasada (tras el envío, antes de la descarga) el usuario cambia el mismo día.
        remote.afterUpsertAssignments = {
            pause()
            a.assignments.setShiftIds(day, listOf("default-night"))
        }
        a.engine.syncNow("u1")

        // La descarga trae la versión antigua que acabamos de enviar: no debe pisar el cambio nuevo ni marcarlo como enviado.
        assertEquals(listOf("default-night"), a.ids())
        assertTrue(a.db.shiftAssignmentDao().getDirty().isNotEmpty())

        a.engine.syncNow("u1"); b.engine.syncNow("u1")
        assertEquals(listOf("default-night"), b.ids())
        assertEquals(0, a.dirtyRows())
    }

    @Test
    fun repaintingAClearedDayBringsItBackOnOtherDevices() = runTest {
        a.assignments.setShiftIds(day, listOf("default-morning"))
        a.engine.syncNow("u1"); b.engine.syncNow("u1")
        pause()
        a.assignments.setShiftIds(day, emptyList())
        a.engine.syncNow("u1"); b.engine.syncNow("u1")
        assertEquals(emptyList(), b.ids())
        pause()
        a.assignments.setShiftIds(day, listOf("default-afternoon")) // se repinta el día borrado
        a.engine.syncNow("u1"); b.engine.syncNow("u1")
        assertEquals(listOf("default-afternoon"), b.ids())
        assertEquals(listOf("default-afternoon"), a.ids())
    }

    @Test
    fun aFailureAfterSendingDoesNotLoseOtherDevicesChanges() = runTest {
        b.assignments.setShiftIds(otherDay, listOf("default-night"))
        b.engine.syncNow("u1")
        a.assignments.setShiftIds(day, listOf("default-morning"))
        // El envío de A llega al servidor pero la conexión cae antes de descargar.
        remote.afterUpsertAssignments = { remote.offline = true }
        a.engine.syncNow("u1")
        assertEquals(SyncState.Failed(offline = true), a.engine.state.value)
        assertEquals(emptyList(), a.ids(otherDay)) // aún no se ha descargado lo de B

        remote.offline = false
        a.engine.syncNow("u1")
        assertEquals(listOf("default-night"), a.ids(otherDay)) // no se saltó nada
        assertEquals(listOf("default-morning"), a.ids())
        assertEquals(0, a.dirtyRows())
    }

    @Test
    fun differentDaysEditedOnBothDevicesMerge() = runTest {
        a.assignments.setShiftIds(day, listOf("default-morning"))
        b.assignments.setShiftIds(otherDay, listOf("default-night"))
        a.engine.syncNow("u1"); b.engine.syncNow("u1"); a.engine.syncNow("u1")
        for (device in listOf(a, b)) {
            assertEquals(listOf("default-morning"), device.ids(day))
            assertEquals(listOf("default-night"), device.ids(otherDay))
        }
    }

    @Test
    fun syncingAgainWithoutChangesIsStable() = runTest {
        a.assignments.setShiftIds(day, listOf("default-morning", "default-night"))
        a.notes.upsert(DayNote("n1", day, NoteKind.Note, null, "curso"))
        repeat(3) { a.engine.syncNow("u1"); b.engine.syncNow("u1") }
        assertEquals(listOf("default-morning", "default-night"), b.ids())
        assertEquals(listOf("curso"), b.notes.observeNotes(day, day).first().map { it.text })
        assertEquals(3, b.shifts.observeShifts().first().size)
        assertEquals(0, a.dirtyRows())
        assertEquals(0, b.dirtyRows())
        assertIs<SyncState.Idle>(b.engine.state.value)
    }

    @Test
    fun lastEditWinsForNotesAndShifts() = runTest {
        a.notes.upsert(DayNote("n1", day, NoteKind.IOwe, "Ana", "v0"))
        a.engine.syncNow("u1"); b.engine.syncNow("u1")
        pause()
        a.notes.upsert(DayNote("n1", day, NoteKind.IOwe, "Ana", "de A"))
        a.shifts.upsert(Shift("default-morning", "Nombre de A", "MA", 0xFF2196F3, 6 * 60, 14 * 60))
        pause()
        b.notes.upsert(DayNote("n1", day, NoteKind.IOwe, "Ana", "de B")) // B edita después
        b.shifts.upsert(Shift("default-morning", "Nombre de B", "MB", 0xFF2196F3, 6 * 60, 14 * 60))
        a.engine.syncNow("u1"); b.engine.syncNow("u1"); a.engine.syncNow("u1")
        assertEquals("de B", a.notes.observeNotes(day, day).first().single().text)
        assertEquals("de B", b.notes.observeNotes(day, day).first().single().text)
        assertEquals("Nombre de B", a.shifts.observeShifts().first().first { it.id == "default-morning" }.name)
    }

    @Test
    fun theFirstAccountAdoptsLocalDataAndStaysAfterResync() = runTest {
        a.assignments.setShiftIds(day, listOf("default-morning")) // se pinta antes de iniciar sesión
        a.engine.syncNow("u1")
        assertTrue(remote.assignments.keys.any { it.first == "u1" })
        a.engine.syncNow("u1") // misma cuenta: no se vacía nada
        assertEquals(listOf("default-morning"), a.ids())
    }

    @Test
    fun switchingAccountAlsoWipesNotesAndRestoresDefaultShifts() = runTest {
        a.notes.upsert(DayNote("n1", day, NoteKind.OwedToMe, "Luis", ""))
        a.shifts.upsert(Shift("custom", "Guardia", "G", 0xFF000000, 8 * 60, 20 * 60))
        a.engine.syncNow("u1")
        remote.user = "u2"
        a.engine.syncNow("u2")
        assertTrue(a.notes.observeDebts().first().isEmpty())
        assertEquals(listOf("M", "T", "N"), a.shifts.observeShifts().first().map { it.abbreviation })
        assertTrue(remote.dayNotes.keys.none { it.first == "u2" })
        assertTrue(remote.shifts.keys.none { it.first == "u2" && it.second == "custom" })
    }

    @Test
    fun anotherUsersDataIsNeverDownloaded() = runTest {
        a.assignments.setShiftIds(day, listOf("default-morning"))
        a.engine.syncNow("u1")
        remote.user = "u2"
        b.engine.syncNow("u2")
        assertEquals(emptyList(), b.ids())
    }
}
