package com.jfaf.calendario.domain.usecase

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** Días entre [from] y [to], ambos incluidos, en cualquier orden. Un rango invertido se trata igual que el directo. */
fun dateRange(from: LocalDate, to: LocalDate): Set<LocalDate> {
    val (start, end) = if (from <= to) from to to else to to from
    return generateSequence(start) { it.plus(1, DateTimeUnit.DAY) }.takeWhile { it <= end }.toSet()
}
