package com.jfaf.calendario.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Turnos asignados a cada día (ids de turno, máximo 2 por día). */
interface ShiftAssignmentRepository {
    fun observeAssignments(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, List<String>>>
    suspend fun getShiftIds(date: LocalDate): List<String>
    suspend fun setShiftIds(date: LocalDate, shiftIds: List<String>)

    /** Asignaciones (sólo días con turnos) del rango, ambos extremos incluidos. */
    suspend fun getAssignments(from: LocalDate, to: LocalDate): Map<LocalDate, List<String>>

    /** Escribe varios días de una vez y de forma atómica; una lista vacía deja el día sin turnos. */
    suspend fun setAssignments(changes: Map<LocalDate, List<String>>)
}
