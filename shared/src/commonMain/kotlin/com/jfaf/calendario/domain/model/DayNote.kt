package com.jfaf.calendario.domain.model

import kotlinx.datetime.LocalDate

/** Tipo de apunte de un día: nota libre, un turno que yo debo a alguien o un turno que alguien me debe. */
enum class NoteKind { Note, IOwe, OwedToMe }

/**
 * Nota de un día. Los apuntes de deuda ([NoteKind.IOwe], [NoteKind.OwedToMe]) representan **un turno** cada uno
 * y llevan la persona implicada en [person]; [settled] indica que ya se devolvió.
 */
data class DayNote(
    val id: String,
    val date: LocalDate,
    val kind: NoteKind,
    val person: String?,
    val text: String,
    val settled: Boolean = false,
) {
    val isDebt: Boolean get() = kind != NoteKind.Note
}

/** Resumen de un día para el calendario: si tiene notas y si tiene deudas aún sin saldar. */
data class DayNoteMarkers(val hasNote: Boolean, val hasOpenDebt: Boolean)

/** Turnos pendientes y saldados con una persona. */
data class PersonBalance(val person: String, val open: List<DayNote>, val settled: List<DayNote>) {
    /** Turnos que le debo (sin saldar). */
    val iOweCount: Int get() = open.count { it.kind == NoteKind.IOwe }

    /** Turnos que me debe (sin saldar). */
    val owedToMeCount: Int get() = open.count { it.kind == NoteKind.OwedToMe }
}

/** Datos del formulario de una nota; `id = null` si es nueva. */
data class DayNoteDraft(
    val id: String?,
    val date: LocalDate,
    val kind: NoteKind,
    val person: String,
    val text: String,
    val settled: Boolean = false,
) {
    /** Motivo por el que no se puede guardar, o `null` si es válida. */
    fun validationError(): DayNoteError? = when {
        person.trim().length > DayNoteError.MAX_PERSON -> DayNoteError.PersonTooLong
        text.trim().length > DayNoteError.MAX_TEXT -> DayNoteError.TextTooLong
        kind == NoteKind.Note && text.isBlank() -> DayNoteError.TextRequired
        kind != NoteKind.Note && person.isBlank() -> DayNoteError.PersonRequired
        else -> null
    }
}

enum class DayNoteError {
    TextRequired, PersonRequired, PersonTooLong, TextTooLong;

    companion object {
        const val MAX_PERSON = 40
        const val MAX_TEXT = 500
    }
}
