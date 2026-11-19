package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.BlockedUser
import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.LinkRequestResult
import com.jfaf.calendario.domain.model.LinkSummary
import com.jfaf.calendario.domain.model.MyShiftOption
import com.jfaf.calendario.domain.model.PendingRequest
import com.jfaf.calendario.domain.model.SharedAssignment
import com.jfaf.calendario.domain.model.SwapProposal
import com.jfaf.calendario.domain.model.isValidAlias
import com.jfaf.calendario.domain.repository.LinkRepository
import com.jfaf.calendario.domain.repository.SharedScheduleRepository
import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import com.jfaf.calendario.domain.repository.ShiftRepository
import com.jfaf.calendario.domain.repository.SocialError
import com.jfaf.calendario.domain.repository.SocialException
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

/** Ventana de días (desde hoy) en la que se ofrecen mis turnos para un cambio. */
const val SWAP_WINDOW_DAYS = 60

/** Todo lo que muestra la sección "Compañeros" de una vez. Un fallo parcial deja su parte vacía. */
data class SocialOverview(
    val alias: String?,
    val pending: List<PendingRequest>,
    val links: List<LinkSummary>,
    val swaps: List<SwapProposal>,
    val blocked: List<BlockedUser>,
)

/** Guarda mi alias. Un formato inválido falla con `InvalidAlias` sin llamar al servidor. */
class SaveAliasUseCase(private val links: LinkRepository) {
    suspend operator fun invoke(input: String): Result<String> {
        val alias = input.trim()
        if (!isValidAlias(alias)) return Result.failure(SocialException(SocialError.InvalidAlias))
        return links.setAlias(alias)
    }
}

/** Pide un vínculo por alias. Mismo criterio de formato que [SaveAliasUseCase]. */
class RequestLinkUseCase(private val links: LinkRepository) {
    suspend operator fun invoke(input: String): Result<LinkRequestResult> {
        val alias = input.trim()
        if (!isValidAlias(alias)) return Result.failure(SocialException(SocialError.InvalidAlias))
        return links.requestLink(alias)
    }
}

class LoadSocialOverviewUseCase(
    private val links: LinkRepository,
    private val schedule: SharedScheduleRepository,
) {
    /** Sin sesión o con fallos, cada parte queda vacía en vez de romper la pantalla. */
    suspend operator fun invoke(): SocialOverview = SocialOverview(
        alias = links.myAlias().getOrNull(),
        pending = links.pendingRequests().getOrDefault(emptyList()),
        links = links.myLinks().getOrDefault(emptyList()),
        swaps = schedule.mySwaps().getOrDefault(emptyList()),
        blocked = links.blockedUsers().getOrDefault(emptyList()),
    )
}

class AcceptRequestUseCase(private val links: LinkRepository) {
    suspend operator fun invoke(linkId: String): Result<Unit> = links.acceptRequest(linkId)
}

class RejectRequestUseCase(private val links: LinkRepository) {
    suspend operator fun invoke(linkId: String): Result<Unit> = links.rejectRequest(linkId)
}

class BlockRequesterUseCase(private val links: LinkRepository) {
    suspend operator fun invoke(linkId: String): Result<Unit> = links.blockRequester(linkId)
}

class UnblockUseCase(private val links: LinkRepository) {
    suspend operator fun invoke(blockedId: String): Result<Unit> = links.unblock(blockedId)
}

class RevokeLinkUseCase(private val links: LinkRepository) {
    suspend operator fun invoke(linkId: String): Result<Unit> = links.revokeLink(linkId)
}

class LoadSharedScheduleUseCase(private val schedule: SharedScheduleRepository) {
    suspend operator fun invoke(ownerId: String, month: CalendarMonth): Result<List<SharedAssignment>> =
        schedule.sharedSchedule(ownerId, month)
}

class ProposeSwapUseCase(private val schedule: SharedScheduleRepository) {
    suspend operator fun invoke(
        targetId: String,
        proposerDate: LocalDate,
        proposerSlot: Int,
        targetDate: LocalDate,
        targetSlot: Int,
    ): Result<Unit> = schedule.proposeSwap(targetId, proposerDate, proposerSlot, targetDate, targetSlot)
}

/** Aceptar (true) o rechazar (false) una propuesta que me han hecho. */
class RespondSwapUseCase(private val schedule: SharedScheduleRepository) {
    suspend operator fun invoke(swapId: String, accept: Boolean): Result<Unit> = schedule.respondSwap(swapId, accept)
}

class CancelSwapUseCase(private val schedule: SharedScheduleRepository) {
    suspend operator fun invoke(swapId: String): Result<Unit> = schedule.cancelSwap(swapId)
}

/**
 * Mis turnos que puedo ofrecer en un cambio: los de [SWAP_WINDOW_DAYS] días desde [from], con su nombre,
 * ordenados por fecha y posición.
 */
class BuildSwapOptionsUseCase(
    private val shifts: ShiftRepository,
    private val assignments: ShiftAssignmentRepository,
) {
    suspend operator fun invoke(from: LocalDate, days: Int = SWAP_WINDOW_DAYS): List<MyShiftOption> {
        val names = shifts.observeShifts().first().associate { it.id to it.name }
        val to = LocalDate.fromEpochDays(from.toEpochDays() + days)
        return assignments.getAssignments(from, to)
            .flatMap { (date, ids) ->
                ids.mapIndexedNotNull { slot, id -> names[id]?.let { MyShiftOption(date, slot, it) } }
            }
            .sortedWith(compareBy({ it.date }, { it.slot }))
    }
}
