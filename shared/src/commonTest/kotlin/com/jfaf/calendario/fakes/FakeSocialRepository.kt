package com.jfaf.calendario.fakes

import com.jfaf.calendario.domain.model.BlockedUser
import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.LinkRequestResult
import com.jfaf.calendario.domain.model.LinkSummary
import com.jfaf.calendario.domain.model.PendingRequest
import com.jfaf.calendario.domain.model.SharedAssignment
import com.jfaf.calendario.domain.model.SwapProposal
import com.jfaf.calendario.domain.repository.SocialError
import com.jfaf.calendario.domain.repository.SocialException
import com.jfaf.calendario.domain.repository.LinkRepository
import com.jfaf.calendario.domain.repository.PushTokenRepository
import com.jfaf.calendario.domain.repository.SharedScheduleRepository
import kotlinx.datetime.LocalDate

/**
 * Falso de [LinkRepository], [SharedScheduleRepository] y [PushTokenRepository] con datos configurables. Si [error] no es null, toda operación falla con él.
 * [calls] registra las operaciones de escritura para comprobar que se llamaron (o no).
 */
class FakeSocialRepository : LinkRepository, SharedScheduleRepository, PushTokenRepository {
    var error: SocialError? = null
    var alias: String? = null
    var pending: List<PendingRequest> = emptyList()
    var links: List<LinkSummary> = emptyList()
    var swaps: List<SwapProposal> = emptyList()
    var blocked: List<BlockedUser> = emptyList()
    var schedule: List<SharedAssignment> = emptyList()
    var linkRequestResult: LinkRequestResult = LinkRequestResult.Sent
    val calls = mutableListOf<String>()

    private fun <T> outcome(value: T): Result<T> =
        error?.let { Result.failure(SocialException(it)) } ?: Result.success(value)

    private fun unit(name: String): Result<Unit> {
        calls += name
        return outcome(Unit)
    }

    override suspend fun setAlias(alias: String): Result<String> {
        calls += "setAlias:$alias"
        return outcome(alias).also { if (it.isSuccess) this.alias = alias }
    }

    override suspend fun myAlias(): Result<String?> = outcome(alias)
    override suspend fun requestLink(alias: String): Result<LinkRequestResult> {
        calls += "requestLink:$alias"
        return outcome(linkRequestResult)
    }
    override suspend fun pendingRequests(): Result<List<PendingRequest>> = outcome(pending)
    override suspend fun myLinks(): Result<List<LinkSummary>> = outcome(links)
    override suspend fun acceptRequest(linkId: String) = unit("accept:$linkId")
    override suspend fun rejectRequest(linkId: String) = unit("reject:$linkId")
    override suspend fun revokeLink(linkId: String) = unit("revoke:$linkId")
    override suspend fun sharedSchedule(ownerId: String, month: CalendarMonth): Result<List<SharedAssignment>> {
        calls += "schedule:$ownerId:${month.year}-${month.monthNumber}"
        return outcome(schedule)
    }
    override suspend fun proposeSwap(
        targetId: String, proposerDate: LocalDate, proposerSlot: Int, targetDate: LocalDate, targetSlot: Int,
    ) = unit("propose:$targetId:$proposerDate:$proposerSlot:$targetDate:$targetSlot")
    override suspend fun respondSwap(swapId: String, accept: Boolean) = unit("respond:$swapId:$accept")
    override suspend fun cancelSwap(swapId: String) = unit("cancel:$swapId")
    override suspend fun mySwaps(): Result<List<SwapProposal>> = outcome(swaps)
    override suspend fun registerPushToken(token: String) = unit("registerPush:$token")
    override suspend fun unregisterPushToken(token: String) = unit("unregisterPush:$token")
    override suspend fun blockRequester(linkId: String) = unit("block:$linkId")
    override suspend fun blockedUsers(): Result<List<BlockedUser>> = outcome(blocked)
    override suspend fun unblock(blockedId: String) = unit("unblock:$blockedId")
}
