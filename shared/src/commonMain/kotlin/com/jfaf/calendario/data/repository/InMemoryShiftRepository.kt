package com.jfaf.calendario.data.repository

import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Implementación provisional en memoria; se sustituirá por Room. */
class InMemoryShiftRepository : ShiftRepository {
    private val shifts = MutableStateFlow(
        listOf(
            Shift(1, "Mañana", "M", 0xFF2196F3),
            Shift(2, "Tarde", "T", 0xFFFF9800),
            Shift(3, "Noche", "N", 0xFF3F51B5),
        )
    )

    override fun observeShifts(): StateFlow<List<Shift>> = shifts

    override suspend fun upsert(shift: Shift) {
        shifts.update { list -> list.filterNot { it.id == shift.id } + shift }
    }
}
