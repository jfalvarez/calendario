package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.DayNoteMarkers
import com.jfaf.calendario.domain.repository.DayNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/** Marcas de notas y deudas por día. Cubre el mes y sus vecinos, para que al deslizar ya lleguen marcados. */
class ObserveNoteMarkersUseCase(private val notes: DayNoteRepository) {
    operator fun invoke(month: CalendarMonth): Flow<Map<LocalDate, DayNoteMarkers>> {
        val before = buildMonthGrid(month.previous, DayOfWeek.MONDAY)
        val after = buildMonthGrid(month.next, DayOfWeek.MONDAY)
        return notes.observeNotes(before.first().first(), after.last().last()).map(::markersOf)
    }
}
