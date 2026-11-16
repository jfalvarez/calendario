package com.jfaf.calendario.domain.usecase

import kotlinx.datetime.LocalDate

/**
 * Festivos de ámbito nacional de España, calculados en código (sin red ni datos que mantener).
 * No incluye festivos autonómicos, ni los trasladados al lunes por cada comunidad, ni los locales.
 */
object SpanishHolidays {
    fun nationalHolidays(year: Int): Map<LocalDate, String> = buildMap {
        put(LocalDate(year, 1, 1), "Año Nuevo")
        put(LocalDate(year, 1, 6), "Epifanía del Señor")
        put(goodFriday(year), "Viernes Santo")
        put(LocalDate(year, 5, 1), "Fiesta del Trabajo")
        put(LocalDate(year, 8, 15), "Asunción de la Virgen")
        put(LocalDate(year, 10, 12), "Fiesta Nacional de España")
        put(LocalDate(year, 11, 1), "Todos los Santos")
        put(LocalDate(year, 12, 6), "Día de la Constitución")
        put(LocalDate(year, 12, 8), "Inmaculada Concepción")
        put(LocalDate(year, 12, 25), "Navidad")
    }

    fun nationalHoliday(date: LocalDate): String? = nationalHolidays(date.year)[date]

    private fun goodFriday(year: Int): LocalDate = LocalDate.fromEpochDays(easterSunday(year).toEpochDays() - 2)

    /** Domingo de Pascua (algoritmo gregoriano anónimo de Meeus/Jones/Butcher). */
    internal fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate(year, month, day)
    }
}
