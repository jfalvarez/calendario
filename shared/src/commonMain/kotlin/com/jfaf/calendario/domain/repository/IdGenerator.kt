package com.jfaf.calendario.domain.repository

/** Genera ids únicos de nuevos registros (UUID en la implementación real). */
fun interface IdGenerator {
    fun newId(): String
}
