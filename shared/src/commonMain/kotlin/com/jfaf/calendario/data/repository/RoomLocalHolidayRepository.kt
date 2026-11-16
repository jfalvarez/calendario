package com.jfaf.calendario.data.repository

import com.jfaf.calendario.data.local.LocalHolidayDao
import com.jfaf.calendario.data.local.LocalHolidayEntity
import com.jfaf.calendario.domain.repository.LocalHolidayRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class RoomLocalHolidayRepository(private val dao: LocalHolidayDao) : LocalHolidayRepository {

    override fun observeHolidays(from: LocalDate, to: LocalDate): Flow<Set<LocalDate>> =
        dao.observeAlive(from.toString(), to.toString()).map { rows -> rows.map(LocalDate::parse).toSet() }

    override suspend fun getHolidays(dates: Set<LocalDate>): Set<LocalDate> =
        dao.getByDates(dates.map { it.toString() }).filter { it.deletedAt == null }.map { LocalDate.parse(it.date) }.toSet()

    override suspend fun setHoliday(dates: Set<LocalDate>, holiday: Boolean) {
        if (dates.isEmpty()) return
        val existing = dao.getByDates(dates.map { it.toString() }).associateBy { it.date }
        val now = nowMillis()
        val rows = dates.mapNotNull { date ->
            val key = date.toString()
            val current = existing[key]
            val alive = current != null && current.deletedAt == null
            when {
                holiday && !alive -> LocalHolidayEntity(key, now, null, true)
                !holiday && alive -> current!!.copy(updatedAt = now, deletedAt = now, dirty = true)
                else -> null
            }
        }
        if (rows.isNotEmpty()) dao.upsertAll(rows)
    }
}
