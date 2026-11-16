package com.jfaf.calendario.domain.model

/** Turno de trabajo configurable. Modelo de dominio puro (sin Android, Compose ni Room). */
data class Shift(
    val id: Long,
    val name: String,
    val abbreviation: String,
    val colorArgb: Long,
)
