package com.jfaf.calendario.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Festivos locales marcados por el usuario (el festivo se identifica sólo por su fecha). */
interface LocalHolidayRepository {
    fun observeHolidays(from: LocalDate, to: LocalDate): Flow<Set<LocalDate>>

    /** Fechas de `dates` que ya son festivo local. */
    suspend fun getHolidays(dates: Set<LocalDate>): Set<LocalDate>

    /** Marca (`holiday = true`) o desmarca todas las fechas, de forma atómica. */
    suspend fun setHoliday(dates: Set<LocalDate>, holiday: Boolean)
}
