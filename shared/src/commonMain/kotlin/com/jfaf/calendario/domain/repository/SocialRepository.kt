package com.jfaf.calendario.domain.repository

import com.jfaf.calendario.domain.model.BlockedUser
import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.LinkRequestResult
import com.jfaf.calendario.domain.model.LinkSummary
import com.jfaf.calendario.domain.model.PendingRequest
import com.jfaf.calendario.domain.model.SharedAssignment
import com.jfaf.calendario.domain.model.SwapProposal
import kotlinx.datetime.LocalDate

/** Errores de dominio de las operaciones sociales. `Unknown` cubre fallos no esperados. */
enum class SocialError {
    AliasTaken, AliasLocked, InvalidAlias, NotAuthenticated, InvalidTransition, Forbidden, LinkNotFound,
    ShiftMissing, AssignmentMissing, AssignmentChanged, TargetBusy, ProposerBusy, LinkRevoked, SwapNotFound,
    Network, Unknown,
}

/** Error de dominio envuelto en `Result.failure`. */
class SocialException(val error: SocialError) : Exception(error.name)

/** Operaciones de compartir turnos contra el servidor. Todas requieren sesión iniciada. */
interface SocialRepository {
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

    /** Turnos asignados de un compañero con vínculo aceptado, en el mes indicado. */
    suspend fun sharedSchedule(ownerId: String, month: CalendarMonth): Result<List<SharedAssignment>>

    /** Propone cambiar mi turno del día `proposerDate` por el del compañero en `targetDate`. */
    suspend fun proposeSwap(targetId: String, proposerDate: LocalDate, proposerSlot: Int, targetDate: LocalDate, targetSlot: Int): Result<Unit>

    /** Acepta (true) o rechaza (false) una propuesta que me han hecho. Al aceptar, se hace el cambio en los dos calendarios. */
    suspend fun respondSwap(swapId: String, accept: Boolean): Result<Unit>

    /** Cancela una propuesta mía que sigue pendiente. */
    suspend fun cancelSwap(swapId: String): Result<Unit>

    suspend fun mySwaps(): Result<List<SwapProposal>>

    /** Asocia el token de avisos de este dispositivo a mi cuenta. */
    suspend fun registerPushToken(token: String): Result<Unit>

    /** Quita el token de avisos de este dispositivo (al cerrar sesión). */
    suspend fun unregisterPushToken(token: String): Result<Unit>

    /** Bloquea a quien me pidió un vínculo: corta el vínculo y evita nuevas solicitudes suyas. */
    suspend fun blockRequester(linkId: String): Result<Unit>

    /** Personas a las que he bloqueado. */
    suspend fun blockedUsers(): Result<List<BlockedUser>>

    /** Quita el bloqueo: la persona puede volver a pedir vínculo. No restaura vínculos anteriores. */
    suspend fun unblock(blockedId: String): Result<Unit>
}

/** Error de dominio de un fallo: el [SocialException] trae el suyo; cualquier otro fallo es `Unknown`. */
fun Throwable.toSocialError(): SocialError = (this as? SocialException)?.error ?: SocialError.Unknown
