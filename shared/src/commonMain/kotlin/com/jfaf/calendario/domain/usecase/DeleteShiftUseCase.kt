package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.repository.ShiftRepository

/** Elimina un turno (borrado lógico). Los días que lo usaban dejan de mostrarlo. */
class DeleteShiftUseCase(private val repository: ShiftRepository) {
    suspend operator fun invoke(id: String) = repository.delete(id)
}
