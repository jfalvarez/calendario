package com.jfaf.calendario.presentation.stats

import kotlinx.datetime.LocalDate

/** `dd/MM/yyyy`. */
fun formatDate(date: LocalDate): String =
    "${date.day.toString().padStart(2, '0')}/${(date.month.ordinal + 1).toString().padStart(2, '0')}/${date.year}"
