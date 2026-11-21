package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.DayNote
import com.jfaf.calendario.domain.model.DayNoteMarkers
import com.jfaf.calendario.domain.model.NoteKind
import com.jfaf.calendario.fakes.InMemoryDayNoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveNoteMarkersUseCaseTest {
    @Test
    fun marksNotesAndOpenDebtsIncludingNeighbourMonths() = runTest {
        val notes = InMemoryDayNoteRepository(
            listOf(
                DayNote("n1", LocalDate(2026, 11, 3), NoteKind.Note, null, "cumple"),
                DayNote("d1", LocalDate(2026, 11, 5), NoteKind.IOwe, "Ana", "turno"),
                DayNote("d2", LocalDate(2026, 11, 6), NoteKind.OwedToMe, "Luis", "turno", settled = true),
                // Mes siguiente: visible al deslizar, así que también se marca.
                DayNote("d3", LocalDate(2026, 12, 2), NoteKind.IOwe, "Ana", "turno"),
                // Fuera de los tres meses: no se consulta.
                DayNote("far", LocalDate(2027, 3, 1), NoteKind.Note, null, "lejos"),
            ),
        )
        val markers = ObserveNoteMarkersUseCase(notes)(CalendarMonth(2026, 11)).first()

        assertEquals(DayNoteMarkers(hasNote = true, hasOpenDebt = false), markers[LocalDate(2026, 11, 3)])
        assertEquals(DayNoteMarkers(hasNote = false, hasOpenDebt = true), markers[LocalDate(2026, 11, 5)])
        assertEquals(DayNoteMarkers(hasNote = false, hasOpenDebt = false), markers[LocalDate(2026, 11, 6)])
        assertEquals(DayNoteMarkers(hasNote = false, hasOpenDebt = true), markers[LocalDate(2026, 12, 2)])
        assertEquals(null, markers[LocalDate(2027, 3, 1)])
    }
}
