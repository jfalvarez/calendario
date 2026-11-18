package com.jfaf.calendario.domain.model

import kotlinx.datetime.LocalDate

/** Festivo de un día: nacional (calculado) o local (marcado por el usuario, sin nombre). */
data class Holiday(val date: LocalDate, val name: String?, val isLocal: Boolean)
