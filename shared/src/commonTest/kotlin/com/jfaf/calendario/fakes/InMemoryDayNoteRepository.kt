package com.jfaf.calendario.fakes

import com.jfaf.calendario.domain.model.DayNote
import com.jfaf.calendario.domain.repository.DayNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

/** Falso en memoria de [DayNoteRepository]. */
class InMemoryDayNoteRepository(initial: List<DayNote> = emptyList()) : DayNoteRepository {
    private val notes = MutableStateFlow(initial)

    override fun observeNotes(from: LocalDate, to: LocalDate): Flow<List<DayNote>> =
        notes.map { all -> all.filter { it.date in from..to }.sortedBy { it.date } }

    override fun observeDebts(): Flow<List<DayNote>> =
        notes.map { all -> all.filter { it.isDebt }.sortedByDescending { it.date } }

    override suspend fun upsert(note: DayNote) {
        notes.update { list -> list.filterNot { it.id == note.id } + note }
    }

    override suspend fun setSettled(id: String, settled: Boolean) {
        notes.update { list -> list.map { if (it.id == id) it.copy(settled = settled) else it } }
    }

    override suspend fun delete(id: String) {
        notes.update { list -> list.filterNot { it.id == id } }
    }

    fun all(): List<DayNote> = notes.value
}
