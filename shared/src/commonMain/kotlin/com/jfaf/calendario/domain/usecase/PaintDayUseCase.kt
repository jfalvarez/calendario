package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import com.jfaf.calendario.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

/**
 * Modo «Pintar». Por defecto asigna el turno al día y, si el día ya tenía sólo ese turno, lo borra.
 * Con `asSecond` alterna el turno como segundo: lo quita si ya estaba; si no, lo añade junto al que tenga
 * el día (máximo 2) siempre que no se solape en horas.
 */
class PaintDayUseCase(
    private val assignments: ShiftAssignmentRepository,
    private val shifts: ShiftRepository,
) {
    /** Devuelve `true` si no se pudo añadir como segundo turno (día lleno o solapado); en ese caso no escribe nada. */
    suspend operator fun invoke(date: LocalDate, shiftId: String, asSecond: Boolean = false): Boolean {
        val current = assignments.getShiftIds(date)
        if (!asSecond) {
            assignments.setShiftIds(date, if (current == listOf(shiftId)) emptyList() else listOf(shiftId))
            return false
        }
        if (shiftId in current) {
            assignments.setShiftIds(date, current - shiftId)
            return false
        }
        if (current.size >= 2) return true
        val byId = shifts.observeShifts().first().associateBy { it.id }
        val incoming = byId[shiftId] ?: return true
        if (current.any { id -> byId[id]?.let { incoming.overlaps(it) } == true }) return true
        assignments.setShiftIds(date, current + shiftId)
        return false
    }
}
