package com.jfaf.calendario.presentation.calendar

import com.jfaf.calendario.domain.model.CalendarDay
import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.DayNote
import com.jfaf.calendario.domain.model.DayNoteMarkers
import com.jfaf.calendario.domain.model.Shift
import kotlinx.datetime.LocalDate

/** Pintar: elegir un turno y tocar días. Editar: seleccionar días y aplicar acciones sobre ellos. */
enum class CalendarMode { Paint, Edit, Notes }

sealed interface CalendarDialog {
    data object Assign : CalendarDialog
    data object Repeat : CalendarDialog
    /** Asignar un segundo turno chocaría en horas con otro del mismo día: [dates] son esos días. */
    data class ShiftConflict(val dates: List<LocalDate>) : CalendarDialog
}

/** Notas del día abierto en el modo Notas, y personas ya usadas en deudas (para sugerirlas al escribir). */
data class NotesDialogState(val date: LocalDate, val notes: List<DayNote>, val knownPeople: List<String>)

/** Estado inmutable que pinta la pantalla (flujo de datos unidireccional). */
data class CalendarUiState(
    val month: CalendarMonth,
    val today: LocalDate,
    val weeks: List<List<CalendarDay>> = emptyList(),
    /** Cuadrículas de los meses vecinos, que se ven mientras se desliza el calendario. */
    val prevWeeks: List<List<CalendarDay>> = emptyList(),
    val nextWeeks: List<List<CalendarDay>> = emptyList(),
    val shifts: List<Shift> = emptyList(),
    /** Turno activo del modo Pintar; null = pintar desactivado. */
    val paintShiftId: String? = null,
    /** Modo Pintar: añadir el turno como segundo en vez de sustituir. */
    val paintAsSecond: Boolean = false,
    val mode: CalendarMode = CalendarMode.Paint,
    /** Días seleccionados (modo Editar). */
    val selection: Set<LocalDate> = emptySet(),
    val hasClipboard: Boolean = false,
    val dialog: CalendarDialog? = null,
    /** Estado de la sincronización con el servidor; `null` si no hay cuenta (no se muestra). */
    val syncIndicator: SyncIndicator? = null,
    /** Marcas de notas y deudas pendientes por día del mes mostrado. */
    val noteMarkers: Map<LocalDate, DayNoteMarkers> = emptyMap(),
    val notesDialog: NotesDialogState? = null,
)
