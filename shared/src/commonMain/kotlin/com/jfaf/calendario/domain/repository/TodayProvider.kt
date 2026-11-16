package com.jfaf.calendario.domain.repository

import kotlinx.datetime.LocalDate

/** Fecha de hoy; abstraída para poder probar sin depender del reloj. */
fun interface TodayProvider {
    fun today(): LocalDate
}
