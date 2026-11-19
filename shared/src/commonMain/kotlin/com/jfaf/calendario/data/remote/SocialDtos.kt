package com.jfaf.calendario.data.remote

import co.touchlab.kermit.Logger
import com.jfaf.calendario.domain.model.LinkDirection
import com.jfaf.calendario.domain.model.LinkStatus
import com.jfaf.calendario.domain.model.LinkSummary
import com.jfaf.calendario.domain.model.SharedAssignment
import com.jfaf.calendario.domain.model.SwapProposal
import com.jfaf.calendario.domain.model.SwapStatus
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private val log = Logger.withTag("Social")

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

internal fun RemoteSharedAssignment.toDomain() = SharedAssignment(
    date = LocalDate.parse(assignmentDate),
    slot = slot,
    shiftName = shiftName,
    abbreviation = abbreviation,
    colorArgb = colorArgb,
    startMinutes = startMinutes,
    endMinutes = endMinutes,
    breakMinutes = breakMinutes,
)

internal fun RemoteSwap.toDomain() = SwapProposal(
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
        "cancelled" -> SwapStatus.Cancelled
        else -> {
            // Un estado nuevo del servidor no debe pasar en silencio como "cancelada".
            log.w { "Estado de propuesta desconocido: $status" }
            SwapStatus.Cancelled
        }
    },
)

internal fun RemoteLink.toDomain() = LinkSummary(
    linkId = linkId,
    direction = if (direction == "incoming") LinkDirection.Incoming else LinkDirection.Outgoing,
    otherId = otherId,
    otherAlias = otherAlias,
    status = when (status) {
        "pending" -> LinkStatus.Pending
        "accepted" -> LinkStatus.Accepted
        "rejected" -> LinkStatus.Rejected
        "revoked" -> LinkStatus.Revoked
        else -> {
            log.w { "Estado de vínculo desconocido: $status" }
            LinkStatus.Revoked
        }
    },
)
