package com.jfaf.calendario.data

import androidx.room.Room
import com.jfaf.calendario.data.local.CalendarDatabase
import com.jfaf.calendario.data.local.buildCalendarDatabase
import com.jfaf.calendario.data.prefs.createPreferencesDataStore
import com.jfaf.calendario.data.remote.RemoteAssignment
import com.jfaf.calendario.data.remote.RemoteDataSource
import com.jfaf.calendario.data.remote.RemoteShift
import com.jfaf.calendario.data.repository.RoomShiftAssignmentRepository
import com.jfaf.calendario.data.repository.RoomShiftRepository
import com.jfaf.calendario.data.sync.SyncEngine
import com.jfaf.calendario.domain.model.AuthState
import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.model.SyncState
import com.jfaf.calendario.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import java.io.File
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Servidor falso que imita a Supabase: filas por (dueño, id), «último cambio gana» y cursor `server_updated_at`. */
private class FakeRemote : RemoteDataSource {
    val shifts = mutableMapOf<Pair<String, String>, RemoteShift>()
    val assignments = mutableMapOf<Pair<String, String>, RemoteAssignment>()
    var offline = false
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
    val shifts = RoomShiftRepository(db.shiftDao())
    val assignments = RoomShiftAssignmentRepository(db.shiftAssignmentDao())
    val engine = SyncEngine(
        db, remote,
        object : AuthRepository {
            override val authState: Flow<AuthState> = emptyFlow()
            override suspend fun signIn(email: String, password: String) = null
            override suspend fun signUp(email: String, password: String) = null
            override suspend fun signOut() {}
        },
        createPreferencesDataStore(prefsFile.absolutePath, scope), scope,
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
