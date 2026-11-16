package com.jfaf.calendario.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Turnos asignados a cada día (ids de turno, máximo 2 por día). */
interface ShiftAssignmentRepository {
    fun observeAssignments(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<Long>>>
    suspend fun getShiftIds(date: LocalDate): List<Long>
    suspend fun setShiftIds(date: LocalDate, shiftIds: List<Long>)
}
