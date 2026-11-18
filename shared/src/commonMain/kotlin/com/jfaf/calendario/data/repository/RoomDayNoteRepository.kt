package com.jfaf.calendario.data.repository

import com.jfaf.calendario.data.local.DayNoteDao
import com.jfaf.calendario.data.local.DayNoteEntity
import com.jfaf.calendario.data.local.toDomain
import com.jfaf.calendario.domain.model.DayNote
import com.jfaf.calendario.domain.repository.DayNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class RoomDayNoteRepository(private val dao: DayNoteDao) : DayNoteRepository {

    override fun observeNotes(from: LocalDate, to: LocalDate): Flow<List<DayNote>> =
        dao.observeAlive(from.toString(), to.toString()).map { rows -> rows.map { it.toDomain() } }

    override fun observeDebts(): Flow<List<DayNote>> = dao.observeDebts().map { rows -> rows.map { it.toDomain() } }

    override suspend fun upsert(note: DayNote) {
        dao.upsert(
            DayNoteEntity(
                id = note.id,
                date = note.date.toString(),
                kind = note.kind.name,
                person = note.person,
                body = note.text,
                settled = note.settled,
                updatedAt = nowMillis(),
                deletedAt = null,
                dirty = true,
            )
        )
    }

    override suspend fun setSettled(id: String, settled: Boolean) {
        val existing = dao.get(id) ?: return
        if (existing.deletedAt != null || existing.settled == settled) return
        dao.upsert(existing.copy(settled = settled, updatedAt = nowMillis(), dirty = true))
    }

    override suspend fun delete(id: String) {
        val existing = dao.get(id) ?: return
        if (existing.deletedAt != null) return
        val now = nowMillis()
        dao.upsert(existing.copy(deletedAt = now, updatedAt = now, dirty = true))
    }
}
