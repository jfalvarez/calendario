package com.jfaf.calendario.data.repository

import com.jfaf.calendario.data.local.ShiftDao
import com.jfaf.calendario.data.local.ShiftEntity
import com.jfaf.calendario.data.local.toDomain
import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.repository.ShiftRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalTime::class)
class RoomShiftRepository(private val dao: ShiftDao, private val clock: Clock) : ShiftRepository {
    override fun observeShifts(): Flow<List<Shift>> =
        dao.observeAlive().map { list -> list.map { it.toDomain() } }

    override suspend fun upsert(shift: Shift) {
        val existing = dao.get(shift.id)
        dao.upsert(
            ShiftEntity(
                id = shift.id,
                name = shift.name,
                abbreviation = shift.abbreviation,
                colorArgb = shift.colorArgb,
                sortOrder = existing?.sortOrder ?: dao.nextSortOrder(),
                updatedAt = clock.now().toEpochMilliseconds(),
                deletedAt = null,
                dirty = true,
                startMinutes = shift.startMinutes,
                endMinutes = shift.endMinutes,
                breakMinutes = shift.breakMinutes,
            )
        )
    }

    override suspend fun delete(id: String) {
        val existing = dao.get(id) ?: return
        if (existing.deletedAt != null) return
        val now = clock.now().toEpochMilliseconds()
        dao.upsert(existing.copy(deletedAt = now, updatedAt = now, dirty = true))
    }

    override suspend fun reorder(orderedIds: List<String>) {
        val now = clock.now().toEpochMilliseconds()
        val byId = dao.getAlive().associateBy { it.id }
        val changed = orderedIds.mapIndexedNotNull { index, id ->
            byId[id]?.takeIf { it.sortOrder != index }?.copy(sortOrder = index, updatedAt = now, dirty = true)
        }
        if (changed.isNotEmpty()) dao.upsertAll(changed)
    }
}
