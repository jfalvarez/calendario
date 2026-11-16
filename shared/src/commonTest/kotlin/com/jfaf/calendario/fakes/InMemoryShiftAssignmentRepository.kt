package com.jfaf.calendario.fakes

import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

/** Falso en memoria para pruebas de dominio y presentación. */
class InMemoryShiftAssignmentRepository : ShiftAssignmentRepository {
    private val data = MutableStateFlow<Map<LocalDate, List<String>>>(emptyMap())

    override fun observeAssignments(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<String>>> =
        data.map { all -> all.filterKeys { it in from..to } }

    override suspend fun getShiftIds(date: LocalDate): List<String> = data.value[date].orEmpty()

    override suspend fun setShiftIds(date: LocalDate, shiftIds: List<String>) {
        data.update { if (shiftIds.isEmpty()) it - date else it + (date to shiftIds.take(2)) }
    }
}
