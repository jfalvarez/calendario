package com.jfaf.calendario.domain.repository

import com.jfaf.calendario.domain.model.Shift
import kotlinx.coroutines.flow.Flow

/** Contrato del dominio; lo implementa la capa de datos. */
interface ShiftRepository {
    /** Turnos no eliminados, en el orden elegido por el usuario. */
    fun observeShifts(): Flow<List<Shift>>
    suspend fun upsert(shift: Shift)
    suspend fun delete(id: String)
    /** Fija el orden: la posición en la lista es el nuevo orden. */
    suspend fun reorder(orderedIds: List<String>)
}
