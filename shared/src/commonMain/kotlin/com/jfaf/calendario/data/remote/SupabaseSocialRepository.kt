package com.jfaf.calendario.data.remote

import com.jfaf.calendario.domain.model.LinkDirection
import com.jfaf.calendario.domain.model.LinkRequestResult
import com.jfaf.calendario.domain.model.LinkStatus
import com.jfaf.calendario.domain.model.LinkSummary
import com.jfaf.calendario.domain.model.PendingRequest
import com.jfaf.calendario.domain.repository.SocialError
import com.jfaf.calendario.domain.repository.SocialException
import com.jfaf.calendario.domain.repository.SocialRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CancellationException
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

    override suspend fun blockRequester(linkId: String): Result<Unit> = call {
        client.postgrest.rpc("block_requester", buildJsonObject { put("p_link", linkId) })
    }

    private suspend fun setStatus(linkId: String, status: String): Result<Unit> = call {
        client.postgrest.rpc(
            "set_link_status",
            buildJsonObject { put("p_link", linkId); put("p_status", status) },
        )
    }

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
        "invalid_alias" in text -> SocialError.InvalidAlias
        "not_authenticated" in text -> SocialError.NotAuthenticated
        "invalid_transition" in text -> SocialError.InvalidTransition
        "forbidden" in text -> SocialError.Forbidden
        "link_not_found" in text -> SocialError.LinkNotFound
        looksOffline(e) -> SocialError.Network
        else -> SocialError.Unknown
    }
}
