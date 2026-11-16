package com.jfaf.calendario.fakes

import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Falso en memoria para pruebas de dominio y presentación. */
class InMemoryShiftRepository : ShiftRepository {
    private val shifts = MutableStateFlow(
        listOf(
            Shift("default-morning", "Mañana", "M", 0xFF2196F3),
            Shift("default-afternoon", "Tarde", "T", 0xFFFF9800),
            Shift("default-night", "Noche", "N", 0xFF3F51B5),
        )
    )

    override fun observeShifts(): StateFlow<List<Shift>> = shifts

    override suspend fun upsert(shift: Shift) {
        shifts.update { list ->
            if (list.any { it.id == shift.id }) list.map { if (it.id == shift.id) shift else it } else list + shift
        }
    }

    override suspend fun delete(id: String) = shifts.update { list -> list.filterNot { it.id == id } }

    override suspend fun reorder(orderedIds: List<String>) =
        shifts.update { list -> orderedIds.mapNotNull { id -> list.find { it.id == id } } }
}
