package com.jfaf.calendario.presentation.calendar

import com.jfaf.calendario.domain.model.CalendarDay
import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.Shift
import kotlinx.datetime.LocalDate

/** Pintar: elegir un turno y tocar días. Editar: seleccionar días y aplicar acciones sobre ellos. */
enum class CalendarMode { Paint, Edit }

enum class CalendarDialog { Assign, Repeat }

/** Estado inmutable que pinta la pantalla (flujo de datos unidireccional). */
data class CalendarUiState(
    val month: CalendarMonth,
    val today: LocalDate,
    val weeks: List<List<CalendarDay>> = emptyList(),
    val shifts: List<Shift> = emptyList(),
    /** Turno activo del modo Pintar; null = pintar desactivado. */
    val paintShiftId: String? = null,
    val mode: CalendarMode = CalendarMode.Paint,
    /** Días seleccionados (modo Editar). */
    val selection: Set<LocalDate> = emptySet(),
    val hasClipboard: Boolean = false,
    val dialog: CalendarDialog? = null,
    /** Estado de la sincronización con el servidor; `null` si no hay cuenta (no se muestra). */
    val syncIndicator: SyncIndicator? = null,
)
