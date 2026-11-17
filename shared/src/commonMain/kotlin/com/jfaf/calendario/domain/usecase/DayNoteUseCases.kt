package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.DayNote
import com.jfaf.calendario.domain.model.DayNoteDraft
import com.jfaf.calendario.domain.model.DayNoteError
import com.jfaf.calendario.domain.model.DayNoteMarkers
import com.jfaf.calendario.domain.model.PersonBalance
import com.jfaf.calendario.domain.repository.DayNoteRepository
import com.jfaf.calendario.domain.repository.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class ObserveDayNotesUseCase(private val notes: DayNoteRepository) {
    operator fun invoke(from: LocalDate, to: LocalDate): Flow<List<DayNote>> = notes.observeNotes(from, to)
}

/** Deudas agrupadas por persona (sin distinguir mayúsculas): primero quien tiene algo pendiente. */
class ObserveDebtsUseCase(private val notes: DayNoteRepository) {
    operator fun invoke(): Flow<List<PersonBalance>> = notes.observeDebts().map(::balancesOf)
}

internal fun balancesOf(debts: List<DayNote>): List<PersonBalance> =
    debts.filter { it.isDebt && !it.person.isNullOrBlank() }
        .groupBy { it.person!!.trim().lowercase() }
        .map { (_, group) ->
            PersonBalance(
                person = group.first().person!!.trim(),
                open = group.filter { !it.settled },
                settled = group.filter { it.settled },
            )
        }
        .sortedWith(compareByDescending<PersonBalance> { it.open.isNotEmpty() }.thenBy { it.person.lowercase() })

/** Marcadores del calendario por fecha: hay nota y/o deuda sin saldar. */
fun markersOf(notes: List<DayNote>): Map<LocalDate, DayNoteMarkers> =
    notes.groupBy { it.date }.mapValues { (_, day) ->
        DayNoteMarkers(hasNote = day.any { !it.isDebt }, hasOpenDebt = day.any { it.isDebt && !it.settled })
    }

/** Crea o edita una nota; devuelve el motivo si el formulario no es válido (y no guarda nada). */
class SaveDayNoteUseCase(private val notes: DayNoteRepository, private val ids: IdGenerator) {
    suspend operator fun invoke(draft: DayNoteDraft): DayNoteError? {
        draft.validationError()?.let { return it }
        val isDebt = draft.kind != com.jfaf.calendario.domain.model.NoteKind.Note
        notes.upsert(
            DayNote(
                id = draft.id ?: ids.newId(),
                date = draft.date,
                kind = draft.kind,
                person = draft.person.trim().takeIf { it.isNotEmpty() },
                text = draft.text.trim(),
                settled = isDebt && draft.settled,
            )
        )
        return null
    }
}

class SetNoteSettledUseCase(private val notes: DayNoteRepository) {
    suspend operator fun invoke(id: String, settled: Boolean) = notes.setSettled(id, settled)
}

class DeleteDayNoteUseCase(private val notes: DayNoteRepository) {
    suspend operator fun invoke(id: String) = notes.delete(id)
}
