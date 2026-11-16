package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.data.repository.InMemoryShiftRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveShiftsUseCaseTest {
    @Test
    fun emitsRepositoryShifts() = runTest {
        val shifts = ObserveShiftsUseCase(InMemoryShiftRepository())().first()
        assertEquals(listOf("M", "T", "N"), shifts.map { it.abbreviation })
    }
}
