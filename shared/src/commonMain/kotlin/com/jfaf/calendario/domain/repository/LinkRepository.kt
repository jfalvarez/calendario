package com.jfaf.calendario.domain.repository

import com.jfaf.calendario.domain.model.BlockedUser
import com.jfaf.calendario.domain.model.LinkRequestResult
import com.jfaf.calendario.domain.model.LinkSummary
import com.jfaf.calendario.domain.model.PendingRequest

/** Alias y vínculos entre compañeros. Todas las operaciones requieren sesión iniciada. */
interface LinkRepository {
    /** Fija o cambia mi alias. Devuelve el alias guardado o el error. */
    suspend fun setAlias(alias: String): Result<String>

    /** Mi alias actual, o null si aún no he elegido ninguno. */
    suspend fun myAlias(): Result<String?>

    suspend fun requestLink(alias: String): Result<LinkRequestResult>
    suspend fun pendingRequests(): Result<List<PendingRequest>>
    suspend fun myLinks(): Result<List<LinkSummary>>

    /** Acepta una solicitud pendiente que me han hecho. */
    suspend fun acceptRequest(linkId: String): Result<Unit>

    /** Rechaza una solicitud pendiente. Permite volver a pedirla tras 7 días. */
    suspend fun rejectRequest(linkId: String): Result<Unit>

    /** Corta un vínculo: cancelar mi solicitud, dejar de ver a alguien, o dejar de compartir con alguien. */
    suspend fun revokeLink(linkId: String): Result<Unit>

    /** Bloquea a quien me pidió un vínculo: corta el vínculo y evita nuevas solicitudes suyas. */
    suspend fun blockRequester(linkId: String): Result<Unit>

    /** Personas a las que he bloqueado. */
    suspend fun blockedUsers(): Result<List<BlockedUser>>

    /** Quita el bloqueo: la persona puede volver a pedir vínculo. No restaura vínculos anteriores. */
    suspend fun unblock(blockedId: String): Result<Unit>
}
