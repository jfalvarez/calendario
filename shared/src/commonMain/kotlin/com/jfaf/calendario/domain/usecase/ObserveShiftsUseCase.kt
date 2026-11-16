package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow

/** Un caso de uso = una acción de negocio, invocable con `useCase()`. */
class ObserveShiftsUseCase(private val repository: ShiftRepository) {
    operator fun invoke(): Flow<List<Shift>> = repository.observeShifts()
}
