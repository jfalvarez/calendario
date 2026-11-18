package com.jfaf.calendario.data.local

import com.jfaf.calendario.domain.model.DayNote
import com.jfaf.calendario.domain.model.NoteKind
import com.jfaf.calendario.domain.model.Shift
import kotlinx.datetime.LocalDate

internal fun ShiftEntity.toDomain() =
    Shift(id, name, abbreviation, colorArgb, startMinutes, endMinutes, breakMinutes)

internal fun DayNoteEntity.toDomain() = DayNote(
    id = id,
    date = LocalDate.parse(date),
    kind = NoteKind.entries.firstOrNull { it.name == kind } ?: NoteKind.Note,
    person = person,
    text = body,
    settled = settled,
)
