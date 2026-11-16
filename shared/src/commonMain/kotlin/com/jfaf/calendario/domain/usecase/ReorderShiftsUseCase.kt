package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.repository.ShiftRepository

class ReorderShiftsUseCase(private val repository: ShiftRepository) {
    suspend operator fun invoke(orderedIds: List<String>) = repository.reorder(orderedIds)
}
