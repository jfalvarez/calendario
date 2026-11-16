package com.jfaf.calendario.domain.repository

import com.jfaf.calendario.domain.model.Shift
import kotlinx.coroutines.flow.Flow

/** Contrato del dominio; lo implementa la capa de datos. */
interface ShiftRepository {
    fun observeShifts(): Flow<List<Shift>>
    suspend fun upsert(shift: Shift)
}
