package com.jfaf.calendario.presentation.calendar

import com.jfaf.calendario.domain.model.Shift

/** Estado inmutable que pinta la pantalla (flujo de datos unidireccional). */
data class CalendarUiState(
    val shifts: List<Shift> = emptyList(),
    val isLoading: Boolean = true,
)
