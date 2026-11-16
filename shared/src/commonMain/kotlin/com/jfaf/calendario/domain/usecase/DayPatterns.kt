package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.DayPattern
import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

internal class SelectionPattern(val start: LocalDate, val pattern: DayPattern)

internal fun LocalDate.plusDays(n: Int): LocalDate = plus(n, DateTimeUnit.DAY)

internal fun daysBetween(from: LocalDate, to: LocalDate): Int = (to.toEpochDays() - from.toEpochDays()).toInt()

/** Lee del repositorio el bloque que cubren los días seleccionados (del primero al último). */
internal suspend fun ShiftAssignmentRepository.patternOf(dates: Set<LocalDate>): SelectionPattern? {
    if (dates.isEmpty()) return null
    val start = dates.min()
    val end = dates.max()
    val assigned = getAssignments(start, end)
    val days = (0..daysBetween(start, end)).map { i ->
        val date = start.plusDays(i)
        if (date in dates) assigned[date].orEmpty() else null
    }
    return SelectionPattern(start, DayPattern(days))
}

/** Escribe el bloque a partir de `start`, saltando los días no copiados. */
internal fun DayPattern.changesFrom(start: LocalDate): Map<LocalDate, List<String>> =
    days.withIndex().mapNotNull { (i, ids) -> ids?.let { start.plusDays(i) to it } }.toMap()
