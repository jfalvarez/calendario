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
import com.jfaf.calendario.data.sync.createSyncEngine
import com.jfaf.calendario.domain.model.AuthState
import com.jfaf.calendario.domain.repository.AuthRepository
import java.io.File
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
/** Servidor falso que imita a Supabase: filas por (dueño, id), «último cambio gana» y cursor `server_updated_at`. */
internal class FakeRemote : RemoteDataSource {
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
internal class Device(remote: FakeRemote) {
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

/** Reloj de prueba que lee [millis] en cada llamada. */
internal fun clockOf(millis: () -> Long): Clock = object : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(millis())
}
