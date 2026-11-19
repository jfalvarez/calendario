@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.jfaf.calendario.data

import androidx.room.Room
import com.jfaf.calendario.data.local.CalendarDatabase
import com.jfaf.calendario.data.local.buildCalendarDatabase
import com.jfaf.calendario.data.prefs.createPreferencesDataStore
import com.jfaf.calendario.data.remote.RemoteAssignment
import com.jfaf.calendario.data.remote.RemoteDataSource
import com.jfaf.calendario.data.remote.RemoteDayNote
import com.jfaf.calendario.data.remote.RemoteLocalHoliday
import com.jfaf.calendario.data.remote.RemoteShift
import com.jfaf.calendario.data.repository.RoomDayNoteRepository
import com.jfaf.calendario.data.repository.RoomLocalHolidayRepository
import com.jfaf.calendario.data.repository.RoomShiftAssignmentRepository
import com.jfaf.calendario.data.repository.RoomShiftRepository
import com.jfaf.calendario.data.sync.SyncEngine
import com.jfaf.calendario.data.sync.createSyncEngine
import com.jfaf.calendario.domain.model.AuthState
import com.jfaf.calendario.domain.model.DayNote
import com.jfaf.calendario.domain.model.NoteKind
import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.model.SyncState
import com.jfaf.calendario.domain.repository.AuthRepository
import java.io.File
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

/** Servidor falso que imita a Supabase: filas por (dueño, id), «último cambio gana» y cursor `server_updated_at`. */
private class FakeRemote : RemoteDataSource {
    val shifts = mutableMapOf<Pair<String, String>, RemoteShift>()
    val assignments = mutableMapOf<Pair<String, String>, RemoteAssignment>()
    var offline = false
    /** Se ejecuta una sola vez justo después de aceptar un envío de asignaciones: simula algo que ocurre a mitad de una sincronización. */
    var afterUpsertAssignments: (suspend () -> Unit)? = null
    private var counter = 0
    private fun stamp() = (++counter).toString().padStart(10, '0')
    private fun check() { if (offline) throw IOException("sin red") }

    override suspend fun upsertShifts(rows: List<RemoteShift>) {
        check()
        rows.forEach { r ->
            val key = r.ownerId to r.id
            if (shifts[key].let { it == null || r.updatedAt >= it.updatedAt }) shifts[key] = r.copy(serverUpdatedAt = stamp())
        }
    }

    override suspend fun upsertAssignments(rows: List<RemoteAssignment>) {
        check()
        rows.forEach { r ->
            val key = r.ownerId to r.id
            if (assignments[key].let { it == null || r.updatedAt >= it.updatedAt }) assignments[key] = r.copy(serverUpdatedAt = stamp())
        }
        afterUpsertAssignments?.let { hook -> afterUpsertAssignments = null; hook() }
    }

    val holidays = mutableMapOf<Pair<String, String>, RemoteLocalHoliday>()
    val dayNotes = mutableMapOf<Pair<String, String>, RemoteDayNote>()

    override suspend fun upsertDayNotes(rows: List<RemoteDayNote>) {
        check()
        rows.forEach { r ->
            val key = r.ownerId to r.id
            if (dayNotes[key].let { it == null || r.updatedAt >= it.updatedAt }) dayNotes[key] = r.copy(serverUpdatedAt = stamp())
        }
    }

    override suspend fun pullDayNotes(since: String?): List<RemoteDayNote> {
        check()
        return dayNotes.values.filter { it.ownerId == user && (since == null || it.serverUpdatedAt!! >= since) }.sortedBy { it.serverUpdatedAt }
    }

    override suspend fun upsertHolidays(rows: List<RemoteLocalHoliday>) {
        check()
        rows.forEach { r ->
            val key = r.ownerId to r.date
            if (holidays[key].let { it == null || r.updatedAt >= it.updatedAt }) holidays[key] = r.copy(serverUpdatedAt = stamp())
        }
    }

    override suspend fun pullHolidays(since: String?): List<RemoteLocalHoliday> {
        check()
        return holidays.values.filter { it.ownerId == user && (since == null || it.serverUpdatedAt!! >= since) }.sortedBy { it.serverUpdatedAt }
    }

    var user = "u1"
    override suspend fun pullShifts(since: String?): List<RemoteShift> {
        check()
        return shifts.values.filter { it.ownerId == user && (since == null || it.serverUpdatedAt!! >= since) }.sortedBy { it.serverUpdatedAt }
    }

    override suspend fun pullAssignments(since: String?): List<RemoteAssignment> {
        check()
        return assignments.values.filter { it.ownerId == user && (since == null || it.serverUpdatedAt!! >= since) }.sortedBy { it.serverUpdatedAt }
    }
}

/** Un «dispositivo»: su propia base de datos Room y sus ajustes, contra el mismo servidor. */
private class Device(remote: FakeRemote) {
    private val dbFile = File.createTempFile("device", ".db")
    private val prefsFile = File.createTempFile("device", ".preferences_pb").also { it.delete() }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val db = buildCalendarDatabase(Room.databaseBuilder<CalendarDatabase>(dbFile.absolutePath))
    val shifts = RoomShiftRepository(db.shiftDao(), Clock.System)
    val assignments = RoomShiftAssignmentRepository(db.shiftAssignmentDao(), Clock.System)
    val holidays = RoomLocalHolidayRepository(db.localHolidayDao(), Clock.System)
    val notes = RoomDayNoteRepository(db.dayNoteDao(), Clock.System)
    val engine = createSyncEngine(
        db, remote,
        object : AuthRepository {
            override val authState: Flow<AuthState> = emptyFlow()
            override suspend fun signIn(email: String, password: String) = null
            override suspend fun signUp(email: String, password: String) = null
            override suspend fun signOut() {}
            override suspend fun sendPasswordReset(email: String) = null
            override suspend fun verifyRecoveryCode(email: String, code: String) = null
            override suspend fun updatePassword(newPassword: String) = null
        },
        createPreferencesDataStore(prefsFile.absolutePath, scope = scope), scope,
    )

    fun close() {
        db.close(); scope.cancel(); dbFile.delete(); prefsFile.delete()
    }
}

class SyncEngineTest {
    private val remote = FakeRemote()
    private val a = Device(remote)
    private val b = Device(remote)
    private val day = LocalDate(2026, 11, 16)

    @AfterTest
    fun tearDown() { a.close(); b.close() }

    private suspend fun Device.ids(d: LocalDate = day) = assignments.getShiftIds(d)
    private fun pause() = Thread.sleep(15) // los cambios se ordenan por reloj (ms)

    @Test
    fun changesFlowFromOneDeviceToTheOther() = runTest {
        a.assignments.setShiftIds(day, listOf("default-morning", "default-night"))
        a.engine.syncNow("u1")
        b.engine.syncNow("u1")
        assertEquals(listOf("default-morning", "default-night"), b.ids())
        assertIs<SyncState.Idle>(b.engine.state.value)
    }

    @Test
    fun defaultShiftsAreNotDuplicatedAcrossDevices() = runTest {
        a.engine.syncNow("u1"); b.engine.syncNow("u1"); a.engine.syncNow("u1")
        assertEquals(listOf("M", "T", "N"), a.shifts.observeShifts().first().map { it.abbreviation })
        assertEquals(listOf("M", "T", "N"), b.shifts.observeShifts().first().map { it.abbreviation })
    }

    @Test
    fun lastWriteWinsWhenBothDevicesPaintTheSameDay() = runTest {
        a.assignments.setShiftIds(day, listOf("default-morning"))
        a.engine.syncNow("u1")
        b.engine.syncNow("u1")
        pause()
        b.assignments.setShiftIds(day, listOf("default-night")) // B edita después
        pause()
        a.assignments.setShiftIds(day, listOf("default-afternoon")) // A edita todavía después
        b.engine.syncNow("u1")        // B sube su cambio (el de A aún no está en el servidor)
        a.engine.syncNow("u1")        // A sube el suyo, más reciente: gana
        b.engine.syncNow("u1")
        a.engine.syncNow("u1")
        assertEquals(listOf("default-afternoon"), a.ids())
        assertEquals(listOf("default-afternoon"), b.ids())
    }

    @Test
    fun deletionsPropagate() = runTest {
        a.assignments.setShiftIds(day, listOf("default-morning"))
        a.engine.syncNow("u1"); b.engine.syncNow("u1")
        assertEquals(listOf("default-morning"), b.ids())
        pause()
        a.assignments.setShiftIds(day, emptyList())
        a.engine.syncNow("u1"); b.engine.syncNow("u1")
        assertEquals(emptyList(), b.ids())
    }

    @Test
    fun localHolidaysAndTheirRemovalPropagate() = runTest {
        a.holidays.setHoliday(setOf(day), true)
        a.engine.syncNow("u1"); b.engine.syncNow("u1")
        assertEquals(setOf(day), b.holidays.getHolidays(setOf(day)))
        pause()
        a.holidays.setHoliday(setOf(day), false)
        a.engine.syncNow("u1"); b.engine.syncNow("u1")
        assertTrue(b.holidays.getHolidays(setOf(day)).isEmpty())
    }

    @Test
    fun dayNotesAndDebtsPropagate() = runTest {
        val note = DayNote("n1", day, NoteKind.IOwe, "Ana", "cambio de guardia")
        a.notes.upsert(note)
        a.engine.syncNow("u1"); b.engine.syncNow("u1")
        assertEquals(listOf(note), b.notes.observeNotes(day, day).first())
        pause()
        b.notes.setSettled("n1", true) // B salda la deuda
        b.engine.syncNow("u1"); a.engine.syncNow("u1")
        assertEquals(true, a.notes.observeDebts().first().single().settled)
        pause()
        a.notes.delete("n1")
        a.engine.syncNow("u1"); b.engine.syncNow("u1")
        assertTrue(b.notes.observeNotes(day, day).first().isEmpty())
    }

    @Test
    fun shiftEditsAndDeletionsPropagate() = runTest {
        a.engine.syncNow("u1"); b.engine.syncNow("u1")
        pause()
        a.shifts.upsert(Shift("default-morning", "Mañana larga", "ML", 0xFF2196F3, 6 * 60, 15 * 60))
        a.shifts.delete("default-night")
        a.engine.syncNow("u1"); b.engine.syncNow("u1")
        val onB = b.shifts.observeShifts().first()
        assertEquals(listOf("Mañana larga", "Tarde"), onB.map { it.name })
    }

    @Test
    fun offlineKeepsLocalChangesAndRecovers() = runTest {
        a.assignments.setShiftIds(day, listOf("default-morning"))
        remote.offline = true
        a.engine.syncNow("u1")
        assertEquals(SyncState.Failed(offline = true), a.engine.state.value)
        assertEquals(listOf("default-morning"), a.ids()) // sigue en local
        assertTrue(a.db.shiftAssignmentDao().getDirty().isNotEmpty()) // y pendiente de enviar
        remote.offline = false
        a.engine.syncNow("u1"); b.engine.syncNow("u1")
        assertTrue(a.db.shiftAssignmentDao().getDirty().isEmpty())
        assertEquals(listOf("default-morning"), b.ids())
    }

    @Test
    fun switchingAccountWipesTheOtherUsersLocalData() = runTest {
        a.assignments.setShiftIds(day, listOf("default-morning"))
        a.engine.syncNow("u1")
        remote.user = "u2"
        a.engine.syncNow("u2") // otra cuenta en el mismo dispositivo
        assertEquals(emptyList(), a.ids()) // los datos de u1 no pasan a u2
        assertTrue(remote.assignments.keys.none { it.first == "u2" })
    }
}

class ForegroundSyncTest {
    @Test
    fun staleness() {
        assertTrue(com.jfaf.calendario.data.sync.isStale(null, 1_000, 30_000))
        assertTrue(!com.jfaf.calendario.data.sync.isStale(1_000, 20_000, 30_000))
        assertTrue(com.jfaf.calendario.data.sync.isStale(1_000, 31_000, 30_000))
        assertTrue(com.jfaf.calendario.data.sync.isStale(50_000, 40_000, 30_000)) // el reloj retrocedió
    }

    @Test
    fun foregroundRequestPullsOnlyWhenStale() = kotlinx.coroutines.runBlocking(Dispatchers.Default) {
        val remote = FakeRemote()
        val phone = Device(remote)
        val pc = Device(remote)
        var now = 1_000_000L
        val auth = object : AuthRepository {
            override val authState = kotlinx.coroutines.flow.MutableStateFlow<AuthState>(AuthState.SignedIn("u1", "a@b.com"))
            override suspend fun signIn(email: String, password: String) = null
            override suspend fun signUp(email: String, password: String) = null
            override suspend fun signOut() {}
            override suspend fun sendPasswordReset(email: String) = null
            override suspend fun verifyRecoveryCode(email: String, code: String) = null
            override suspend fun updatePassword(newPassword: String) = null
        }
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val prefsFile = File.createTempFile("foreground", ".preferences_pb").also { it.delete() }
        val engine = createSyncEngine(
            phone.db, remote, auth, createPreferencesDataStore(prefsFile.absolutePath, scope = scope), scope,
            periodMs = 3_600_000, debounceMs = 3_600_000, clock = clockOf { now }, // sólo cuenta el disparador de primer plano
        )
        suspend fun waitUntil(what: String, check: suspend () -> Boolean) {
            kotlinx.coroutines.withTimeout(10_000) { while (!check()) kotlinx.coroutines.delay(50) }
        }
        try {
            val day = LocalDate(2026, 11, 20)
            engine.start()
            waitUntil("primera sincronización") { engine.state.value is SyncState.Idle && (engine.state.value as SyncState.Idle).lastSyncedAt != null }

            // El PC pinta un día y lo sube; el móvil no se entera aún.
            pc.assignments.setShiftIds(day, listOf("default-morning"))
            pc.engine.syncNow("u1")

            now += 5_000 // vuelve a primer plano a los 5 s: aún no toca
            engine.requestSyncIfStale()
            kotlinx.coroutines.delay(400)
            assertEquals(emptyList(), phone.assignments.getShiftIds(day))

            now += 60_000 // vuelve a primer plano pasado un minuto: sincroniza
            engine.requestSyncIfStale()
            waitUntil("novedades del PC") { phone.assignments.getShiftIds(day) == listOf("default-morning") }
        } finally {
            scope.cancel(); phone.close(); pc.close(); prefsFile.delete()
        }
    }
}

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

/** Reloj de prueba que lee [millis] en cada llamada. */
private fun clockOf(millis: () -> Long): Clock = object : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(millis())
}
