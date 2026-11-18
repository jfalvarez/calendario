package com.jfaf.calendario.domain.repository

import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.LinkRequestResult
import com.jfaf.calendario.domain.model.LinkSummary
import com.jfaf.calendario.domain.model.PendingRequest
import com.jfaf.calendario.domain.model.SharedAssignment

/** Errores de dominio de las operaciones sociales. `Unknown` cubre fallos no esperados. */
enum class SocialError { AliasTaken, InvalidAlias, NotAuthenticated, InvalidTransition, Forbidden, LinkNotFound, Network, Unknown }

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

    /** Bloquea a quien me pidió un vínculo: corta el vínculo y evita nuevas solicitudes suyas. */
    suspend fun blockRequester(linkId: String): Result<Unit>
}
