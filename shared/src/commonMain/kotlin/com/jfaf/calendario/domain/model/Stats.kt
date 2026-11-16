package com.jfaf.calendario.domain.model

import kotlinx.datetime.LocalDate

/** Resumen de un turno dentro de un periodo. */
data class ShiftStats(val shift: Shift, val count: Int, val minutes: Int)

/** Estadísticas de un periodo (`from`..`to`, ambos incluidos). */
data class PeriodStats(
    val from: LocalDate,
    val to: LocalDate,
    val perShift: List<ShiftStats>,
    val daysWorked: Int,
    val totalShifts: Int,
    val totalMinutes: Int,
)

/** Periodos predefinidos del selector. `Custom` lo fija el usuario. */
enum class StatsPeriod { ThisMonth, LastMonth, ThisYear, Last30Days, Custom }
