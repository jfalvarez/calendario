package com.jfaf.calendario.domain.repository

import com.jfaf.calendario.domain.model.DayNote
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface DayNoteRepository {
    /** Notas no eliminadas de las fechas del rango (ambos extremos incluidos). */
    fun observeNotes(from: LocalDate, to: LocalDate): Flow<List<DayNote>>

    /** Todos los apuntes de deuda no eliminados (saldados o no), de más reciente a más antiguo. */
    fun observeDebts(): Flow<List<DayNote>>

    suspend fun upsert(note: DayNote)
    suspend fun setSettled(id: String, settled: Boolean)
    suspend fun delete(id: String)
}
