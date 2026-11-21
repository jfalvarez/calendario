package com.jfaf.calendario.data.sync

import com.jfaf.calendario.data.local.DayNoteDao
import com.jfaf.calendario.data.local.LocalHolidayDao
import com.jfaf.calendario.data.local.ShiftAssignmentDao
import com.jfaf.calendario.data.local.ShiftDao
import com.jfaf.calendario.data.remote.RemoteDataSource
import kotlinx.coroutines.flow.Flow

/** Turnos (catálogo del usuario). */
class ShiftSyncTable(private val dao: ShiftDao, private val remote: RemoteDataSource) : SyncTable {
    override val name = "shifts"
    override fun observeDirtyCount(): Flow<Int> = dao.observeDirtyCount()

    override suspend fun push(userId: String) {
        val rows = dao.getDirty()
        if (rows.isEmpty()) return
        remote.upsertShifts(rows.map { it.toRemote(userId) })
        rows.forEach { dao.markClean(it.id, it.updatedAt) }
    }

    override suspend fun pull(userId: String, cursor: String?): String? {
        val rows = remote.pullShifts(cursor)
        if (rows.isEmpty()) return null
        val local = dao.getByIds(rows.map { it.id }).associateBy { it.id }
        dao.upsertAll(rows.mapNotNull { mergeShift(local[it.id], it) })
        return rows.last().serverUpdatedAt
    }

    override suspend fun clearLocal() = dao.deleteAll()
}

/** Turno asignado a un día (hasta dos por día). */
class AssignmentSyncTable(private val dao: ShiftAssignmentDao, private val remote: RemoteDataSource) : SyncTable {
    override val name = "assignments"
    override fun observeDirtyCount(): Flow<Int> = dao.observeDirtyCount()

    override suspend fun push(userId: String) {
        val rows = dao.getDirty()
        if (rows.isEmpty()) return
        remote.upsertAssignments(rows.map { it.toRemote(userId) })
        rows.forEach { dao.markClean(it.id, it.updatedAt) }
    }

    override suspend fun pull(userId: String, cursor: String?): String? {
        val rows = remote.pullAssignments(cursor)
        if (rows.isEmpty()) return null
        val local = dao.getByIds(rows.map { it.id }).associateBy { it.id }
        dao.upsertAll(rows.mapNotNull { mergeAssignment(local[it.id], it) })
        return rows.last().serverUpdatedAt
    }

    override suspend fun clearLocal() = dao.deleteAll()
}

/** Festivos locales (la clave es la fecha). */
class HolidaySyncTable(private val dao: LocalHolidayDao, private val remote: RemoteDataSource) : SyncTable {
    override val name = "holidays"
    override fun observeDirtyCount(): Flow<Int> = dao.observeDirtyCount()

    override suspend fun push(userId: String) {
        val rows = dao.getDirty()
        if (rows.isEmpty()) return
        remote.upsertHolidays(rows.map { it.toRemote(userId) })
        rows.forEach { dao.markClean(it.date, it.updatedAt) }
    }

    override suspend fun pull(userId: String, cursor: String?): String? {
        val rows = remote.pullHolidays(cursor)
        if (rows.isEmpty()) return null
        val local = dao.getByDates(rows.map { it.date }).associateBy { it.date }
        dao.upsertAll(rows.mapNotNull { mergeHoliday(local[it.date], it) })
        return rows.last().serverUpdatedAt
    }

    override suspend fun clearLocal() = dao.deleteAll()
}

/** Notas y deudas de un día. */
class DayNoteSyncTable(private val dao: DayNoteDao, private val remote: RemoteDataSource) : SyncTable {
    override val name = "day_notes"
    override fun observeDirtyCount(): Flow<Int> = dao.observeDirtyCount()

    override suspend fun push(userId: String) {
        val rows = dao.getDirty()
        if (rows.isEmpty()) return
        remote.upsertDayNotes(rows.map { it.toRemote(userId) })
        rows.forEach { dao.markClean(it.id, it.updatedAt) }
    }

    override suspend fun pull(userId: String, cursor: String?): String? {
        val rows = remote.pullDayNotes(cursor)
        if (rows.isEmpty()) return null
        val local = dao.getByIds(rows.map { it.id }).associateBy { it.id }
        dao.upsertAll(rows.mapNotNull { mergeDayNote(local[it.id], it) })
        return rows.last().serverUpdatedAt
    }

    override suspend fun clearLocal() = dao.deleteAll()
}
