package com.jfaf.calendario.data.repository

import com.jfaf.calendario.data.local.ShiftAssignmentDao
import com.jfaf.calendario.data.local.ShiftAssignmentEntity
import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class RoomShiftAssignmentRepository(private val dao: ShiftAssignmentDao) : ShiftAssignmentRepository {

    override fun observeAssignments(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<String>>> =
        dao.observeAlive(from.toString(), to.toString()).map { rows ->
            rows.groupBy({ LocalDate.parse(it.date) }, { it.shiftId })
        }

    override suspend fun getShiftIds(date: LocalDate): List<String> =
        dao.getForDate(date.toString())
            .filter { it.deletedAt == null }
            .sortedBy { it.slot }
            .map { it.shiftId }

    override suspend fun setShiftIds(date: LocalDate, shiftIds: List<String>) {
        val key = date.toString()
        val existingBySlot = dao.getForDate(key).associateBy { it.slot }
        val now = nowMillis()
        val changes = (0 until MAX_SHIFTS_PER_DAY).mapNotNull { slot ->
            val wanted = shiftIds.getOrNull(slot)
            val current = existingBySlot[slot]
            when {
                wanted != null && (current == null || current.deletedAt != null || current.shiftId != wanted) ->
                    ShiftAssignmentEntity(current?.id ?: newId(), key, slot, wanted, now, null, true)
                wanted == null && current != null && current.deletedAt == null ->
                    current.copy(updatedAt = now, deletedAt = now, dirty = true)
                else -> null
            }
        }
        if (changes.isNotEmpty()) dao.upsertAll(changes)
    }

    private companion object {
        const val MAX_SHIFTS_PER_DAY = 2
    }
}
