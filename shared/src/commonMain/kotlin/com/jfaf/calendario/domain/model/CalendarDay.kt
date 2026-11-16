package com.jfaf.calendario.domain.model

import kotlinx.datetime.LocalDate

/** Celda del calendario: fecha, si pertenece al mes mostrado y sus turnos (máximo 2). */
data class CalendarDay(
    val date: LocalDate,
    val inCurrentMonth: Boolean,
    val shifts: List<Shift>,
)
