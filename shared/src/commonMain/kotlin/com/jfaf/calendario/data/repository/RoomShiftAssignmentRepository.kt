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
        getAssignments(date, date)[date].orEmpty()

    override suspend fun setShiftIds(date: LocalDate, shiftIds: List<String>) =
        setAssignments(mapOf(date to shiftIds))

    override suspend fun getAssignments(from: LocalDate, to: LocalDate): Map<LocalDate, List<String>> =
        dao.getInRange(from.toString(), to.toString())
            .filter { it.deletedAt == null }
            .sortedWith(compareBy({ it.date }, { it.slot }))
            .groupBy({ LocalDate.parse(it.date) }, { it.shiftId })

    override suspend fun setAssignments(changes: Map<LocalDate, List<String>>) {
        if (changes.isEmpty()) return
        val existing = dao.getInRange(changes.keys.min().toString(), changes.keys.max().toString())
            .groupBy { it.date }
        val now = nowMillis()
        val rows = changes.flatMap { (date, ids) ->
            val key = date.toString()
            val bySlot = existing[key].orEmpty().associateBy { it.slot }
            (0 until MAX_SHIFTS_PER_DAY).mapNotNull { slot -> diff(key, slot, ids.getOrNull(slot), bySlot[slot], now) }
        }
        if (rows.isNotEmpty()) dao.upsertAll(rows)
    }

    /** Fila a escribir para que la posición `slot` del día quede como `wanted`, o null si ya lo está. */
    private fun diff(
        date: String, slot: Int, wanted: String?, current: ShiftAssignmentEntity?, now: Long,
    ): ShiftAssignmentEntity? = when {
        wanted != null && (current == null || current.deletedAt != null || current.shiftId != wanted) ->
            ShiftAssignmentEntity(current?.id ?: assignmentId(date, slot), date, slot, wanted, now, null, true)
        wanted == null && current != null && current.deletedAt == null ->
            current.copy(updatedAt = now, deletedAt = now, dirty = true)
        else -> null
    }

    private companion object {
        const val MAX_SHIFTS_PER_DAY = 2
    }
}
