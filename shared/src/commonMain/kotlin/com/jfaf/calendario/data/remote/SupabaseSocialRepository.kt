package com.jfaf.calendario.data.remote

import com.jfaf.calendario.domain.model.BlockedUser
import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.LinkRequestResult
import com.jfaf.calendario.domain.model.LinkSummary
import com.jfaf.calendario.domain.model.PendingRequest
import com.jfaf.calendario.domain.model.SharedAssignment
import com.jfaf.calendario.domain.model.SwapProposal
import com.jfaf.calendario.domain.repository.SocialException
import com.jfaf.calendario.domain.repository.LinkRepository
import com.jfaf.calendario.domain.repository.PushTokenRepository
import com.jfaf.calendario.domain.repository.SharedScheduleRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Implementación contra Supabase: funciones RPC de `supabase/social.sql`. Los DTOs y su mapeo están en `SocialDtos.kt`. */
class SupabaseSocialRepository(private val client: SupabaseClient) :
    LinkRepository,
    SharedScheduleRepository,
    PushTokenRepository {

    override suspend fun setAlias(alias: String): Result<String> = call {
        client.postgrest.rpc("set_my_alias", buildJsonObject { put("p_alias", alias.trim()) })
            .decodeAs<String>()
    }

    override suspend fun myAlias(): Result<String?> = call {
        client.from("profiles").select().decodeList<RemoteProfile>().firstOrNull()?.alias
    }

    override suspend fun requestLink(alias: String): Result<LinkRequestResult> = call {
        val code = client.postgrest.rpc("request_link", buildJsonObject { put("p_alias", alias.trim()) })
            .decodeAs<String>()
        when (code) {
            "sent" -> LinkRequestResult.Sent
            "alias_not_found" -> LinkRequestResult.AliasNotFound
            "self" -> LinkRequestResult.Self
            "too_many_attempts" -> LinkRequestResult.TooManyAttempts
            "too_many_pending" -> LinkRequestResult.TooManyPending
            else -> error("Código de solicitud desconocido: $code")
        }
    }

    override suspend fun pendingRequests(): Result<List<PendingRequest>> = call {
        client.postgrest.rpc("list_pending_requests")
            .decodeList<RemotePendingRequest>()
            .map { PendingRequest(it.linkId, it.requesterAlias.orEmpty()) }
    }

    override suspend fun myLinks(): Result<List<LinkSummary>> = call {
        client.postgrest.rpc("list_my_links")
            .decodeList<RemoteLink>()
            .map { it.toDomain() }
    }

    override suspend fun acceptRequest(linkId: String): Result<Unit> = setStatus(linkId, "accepted")
    override suspend fun rejectRequest(linkId: String): Result<Unit> = setStatus(linkId, "rejected")
    override suspend fun revokeLink(linkId: String): Result<Unit> = setStatus(linkId, "revoked")

    override suspend fun blockedUsers(): Result<List<BlockedUser>> = call {
        client.postgrest.rpc("list_blocked").decodeList<RemoteBlocked>()
            .map { BlockedUser(it.blockedId, it.blockedAlias) }
    }

    override suspend fun unblock(blockedId: String): Result<Unit> = call {
        client.postgrest.rpc("unblock_requester", buildJsonObject { put("p_blocked", blockedId) })
    }

    override suspend fun blockRequester(linkId: String): Result<Unit> = call {
        client.postgrest.rpc("block_requester", buildJsonObject { put("p_link", linkId) })
    }

    private suspend fun setStatus(linkId: String, status: String): Result<Unit> = call {
        client.postgrest.rpc(
            "set_link_status",
            buildJsonObject { put("p_link", linkId); put("p_status", status) },
        )
    }

    override suspend fun sharedSchedule(ownerId: String, month: CalendarMonth): Result<List<SharedAssignment>> = call {
        client.postgrest.rpc(
            "get_shared_schedule",
            buildJsonObject {
                put("p_owner", ownerId)
                put("p_from", month.firstDay.toString())
                put("p_to", LocalDate.fromEpochDays(month.next.firstDay.toEpochDays() - 1).toString())
            },
        ).decodeList<RemoteSharedAssignment>().map { it.toDomain() }
    }

    override suspend fun proposeSwap(targetId: String, proposerDate: LocalDate, proposerSlot: Int, targetDate: LocalDate, targetSlot: Int): Result<Unit> = call {
        client.postgrest.rpc(
            "propose_swap",
            buildJsonObject {
                put("p_target", targetId)
                put("p_proposer_date", proposerDate.toString())
                put("p_proposer_slot", proposerSlot)
                put("p_target_date", targetDate.toString())
                put("p_target_slot", targetSlot)
            },
        )
    }

    override suspend fun respondSwap(swapId: String, accept: Boolean): Result<Unit> = call {
        client.postgrest.rpc("respond_swap", buildJsonObject { put("p_swap", swapId); put("p_accept", accept) })
    }

    override suspend fun cancelSwap(swapId: String): Result<Unit> = call {
        client.postgrest.rpc("cancel_swap", buildJsonObject { put("p_swap", swapId) })
    }

    override suspend fun mySwaps(): Result<List<SwapProposal>> = call {
        client.postgrest.rpc("list_my_swaps").decodeList<RemoteSwap>().map { it.toDomain() }
    }

    override suspend fun registerPushToken(token: String): Result<Unit> = call {
        client.postgrest.rpc("register_device_token", buildJsonObject { put("p_token", token); put("p_platform", "android") })
    }

    override suspend fun unregisterPushToken(token: String): Result<Unit> = call {
        client.postgrest.rpc("unregister_device_token", buildJsonObject { put("p_token", token) })
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(SocialException(socialErrorOf(e)))
    }
}
