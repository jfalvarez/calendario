package com.jfaf.calendario.domain.repository

/** Errores de dominio de las operaciones sociales. `Unknown` cubre fallos no esperados. */
enum class SocialError {
    AliasTaken, AliasLocked, InvalidAlias, NotAuthenticated, InvalidTransition, Forbidden, LinkNotFound,
    ShiftMissing, AssignmentMissing, AssignmentChanged, TargetBusy, ProposerBusy, LinkRevoked, SwapNotFound,
    Network, Unknown,
}

/** Error de dominio envuelto en `Result.failure`. */
class SocialException(val error: SocialError) : Exception(error.name)

/** Error de dominio de un fallo: el [SocialException] trae el suyo; cualquier otro fallo es `Unknown`. */
fun Throwable.toSocialError(): SocialError = (this as? SocialException)?.error ?: SocialError.Unknown
