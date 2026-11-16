package com.jfaf.calendario.presentation.calendar

import com.jfaf.calendario.domain.model.CalendarDay
import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.Shift
import kotlinx.datetime.LocalDate

/** Estado inmutable que pinta la pantalla (flujo de datos unidireccional). */
data class CalendarUiState(
    val month: CalendarMonth,
    val today: LocalDate,
    val weeks: List<List<CalendarDay>> = emptyList(),
    val shifts: List<Shift> = emptyList(),
    /** Turno activo del modo Pintar; null = pintar desactivado. */
    val paintShiftId: Long? = null,
)
