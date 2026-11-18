package com.jfaf.calendario.data.remote

import com.jfaf.calendario.domain.model.BlockedUser
import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.LinkDirection
import com.jfaf.calendario.domain.model.LinkRequestResult
import com.jfaf.calendario.domain.model.LinkStatus
import com.jfaf.calendario.domain.model.LinkSummary
import com.jfaf.calendario.domain.model.PendingRequest
import com.jfaf.calendario.domain.model.SharedAssignment
import com.jfaf.calendario.domain.model.SwapProposal
import com.jfaf.calendario.domain.model.SwapStatus
import com.jfaf.calendario.domain.repository.SocialError
import com.jfaf.calendario.domain.repository.SocialException
import com.jfaf.calendario.domain.repository.SocialRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Fila de `public.profiles` (sólo se lee la propia, por RLS). */
@Serializable
internal data class RemoteProfile(val alias: String? = null)

/** Fila de `public.list_pending_requests()`. */
@Serializable
internal data class RemotePendingRequest(
    @SerialName("link_id") val linkId: String,
    @SerialName("requester_alias") val requesterAlias: String? = null,
)

/** Fila de `public.list_my_links()`. */
@Serializable
internal data class RemoteLink(
    @SerialName("link_id") val linkId: String,
    val direction: String,
    @SerialName("other_id") val otherId: String,
    @SerialName("other_alias") val otherAlias: String? = null,
    val status: String,
)

/** Fila de `public.get_shared_schedule()`. */
@Serializable
internal data class RemoteSharedAssignment(
    @SerialName("assignment_date") val assignmentDate: String,
    val slot: Int,
    @SerialName("shift_id") val shiftId: String? = null,
    @SerialName("shift_name") val shiftName: String,
    val abbreviation: String,
    @SerialName("color_argb") val colorArgb: Long,
    @SerialName("start_minutes") val startMinutes: Int? = null,
    @SerialName("end_minutes") val endMinutes: Int? = null,
    @SerialName("break_minutes") val breakMinutes: Int = 0,
)

/** Fila de `public.list_blocked()`. */
@Serializable
internal data class RemoteBlocked(
    @SerialName("blocked_id") val blockedId: String,
    @SerialName("blocked_alias") val blockedAlias: String? = null,
)

/** Fila de `public.list_my_swaps()`. */
@Serializable
internal data class RemoteSwap(
    @SerialName("swap_id") val swapId: String,
    val direction: String,
    @SerialName("other_alias") val otherAlias: String? = null,
    @SerialName("proposer_date") val proposerDate: String,
    @SerialName("proposer_slot") val proposerSlot: Int,
    @SerialName("proposer_shift") val proposerShift: String? = null,
    @SerialName("target_date") val targetDate: String,
    @SerialName("target_slot") val targetSlot: Int,
    @SerialName("target_shift") val targetShift: String? = null,
    val status: String,
)

/** Implementación contra Supabase: funciones RPC de `supabase/social.sql`. */
class SupabaseSocialRepository(private val client: SupabaseClient) : SocialRepository {

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

    private fun RemoteSharedAssignment.toDomain() = SharedAssignment(
        date = LocalDate.parse(assignmentDate),
        slot = slot,
        shiftName = shiftName,
        abbreviation = abbreviation,
        colorArgb = colorArgb,
        startMinutes = startMinutes,
        endMinutes = endMinutes,
        breakMinutes = breakMinutes,
    )

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

    private fun RemoteSwap.toDomain() = SwapProposal(
        id = swapId,
        direction = if (direction == "outgoing") LinkDirection.Outgoing else LinkDirection.Incoming,
        otherAlias = otherAlias,
        proposerDate = LocalDate.parse(proposerDate),
        proposerSlot = proposerSlot,
        proposerShift = proposerShift,
        targetDate = LocalDate.parse(targetDate),
        targetSlot = targetSlot,
        targetShift = targetShift,
        status = when (status) {
            "pending" -> SwapStatus.Pending
            "accepted" -> SwapStatus.Accepted
            "rejected" -> SwapStatus.Rejected
            else -> SwapStatus.Cancelled
        },
    )

    private fun RemoteLink.toDomain() = LinkSummary(
        linkId = linkId,
        direction = if (direction == "incoming") LinkDirection.Incoming else LinkDirection.Outgoing,
        otherId = otherId,
        otherAlias = otherAlias,
        status = when (status) {
            "pending" -> LinkStatus.Pending
            "accepted" -> LinkStatus.Accepted
            "rejected" -> LinkStatus.Rejected
            else -> LinkStatus.Revoked
        },
    )

    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(SocialException(socialErrorOf(e)))
    }
}

/** Traduce el mensaje de Postgres (código de `social.sql`) a un error de dominio. */
internal fun socialErrorOf(e: Throwable): SocialError {
    val text = when (e) {
        is RestException -> "${e.error} ${e.description.orEmpty()}"
        else -> e.message.orEmpty()
    }.lowercase()
    return when {
        "alias_taken" in text -> SocialError.AliasTaken
        "alias_locked" in text -> SocialError.AliasLocked
        "shift_missing" in text -> SocialError.ShiftMissing
        "assignment_missing" in text -> SocialError.AssignmentMissing
        "assignment_changed" in text -> SocialError.AssignmentChanged
        "target_busy" in text -> SocialError.TargetBusy
        "proposer_busy" in text -> SocialError.ProposerBusy
        "link_revoked" in text -> SocialError.LinkRevoked
        "swap_not_found" in text -> SocialError.SwapNotFound
        "invalid_alias" in text -> SocialError.InvalidAlias
        "not_authenticated" in text -> SocialError.NotAuthenticated
        "invalid_transition" in text -> SocialError.InvalidTransition
        "forbidden" in text -> SocialError.Forbidden
        "link_not_found" in text -> SocialError.LinkNotFound
        looksOffline(e) -> SocialError.Network
        else -> SocialError.Unknown
    }
}
