package com.jfaf.calendario.domain.repository

import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.SharedAssignment
import com.jfaf.calendario.domain.model.SwapProposal
import kotlinx.datetime.LocalDate

/** Calendario compartido de un compañero y cambios de turno entre dos personas. */
interface SharedScheduleRepository {
    /** Turnos asignados de un compañero con vínculo aceptado, en el mes indicado. */
    suspend fun sharedSchedule(ownerId: String, month: CalendarMonth): Result<List<SharedAssignment>>

    /** Propone cambiar mi turno del día `proposerDate` por el del compañero en `targetDate`. */
    suspend fun proposeSwap(
        targetId: String,
        proposerDate: LocalDate,
        proposerSlot: Int,
        targetDate: LocalDate,
        targetSlot: Int,
    ): Result<Unit>

    /** Acepta (true) o rechaza (false) una propuesta que me han hecho. Al aceptar, se hace el cambio en los dos calendarios. */
    suspend fun respondSwap(swapId: String, accept: Boolean): Result<Unit>

    /** Cancela una propuesta mía que sigue pendiente. */
    suspend fun cancelSwap(swapId: String): Result<Unit>

    suspend fun mySwaps(): Result<List<SwapProposal>>
}
