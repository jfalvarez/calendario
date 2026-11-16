package com.jfaf.calendario.data.repository

import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

/** Implementación provisional en memoria; se sustituirá por Room. */
class InMemoryShiftAssignmentRepository : ShiftAssignmentRepository {
    private val data = MutableStateFlow<Map<LocalDate, List<Long>>>(emptyMap())

    override fun observeAssignments(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<Long>>> =
        data.map { all -> all.filterKeys { it in from..to } }

    override suspend fun getShiftIds(date: LocalDate): List<Long> = data.value[date].orEmpty()

    override suspend fun setShiftIds(date: LocalDate, shiftIds: List<Long>) {
        data.update { if (shiftIds.isEmpty()) it - date else it + (date to shiftIds.take(2)) }
    }
}
