package com.jfaf.calendario.domain.model

/** Estado de la sesión del usuario. La cuenta es opcional: sin ella la app funciona sólo en local. */
sealed interface AuthState {
    /** Leyendo la sesión guardada. */
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val userId: String, val email: String?) : AuthState
}

enum class AuthError {
    InvalidEmail, WeakPassword, InvalidCredentials, EmailAlreadyUsed, Network, Unknown,

    /** El código de recuperación es incorrecto o ha caducado. */
    InvalidCode,
    /** Las dos contraseñas nuevas no coinciden. */
    PasswordMismatch,
    /** Demasiados correos o intentos seguidos: hay que esperar. */
    TooManyRequests,

    /** La cuenta se creó pero hay que confirmar el correo antes de poder iniciar sesión. */
    EmailConfirmationRequired,
}

/** Estado de la sincronización con el servidor. */
sealed interface SyncState {
    data object NotSignedIn : SyncState
    /** `lastSyncedAt`: ms UTC de la última sincronización correcta en esta sesión (null si aún no hubo). */
    data class Idle(val lastSyncedAt: Long?) : SyncState
    data object Syncing : SyncState
    /** `offline`: el fallo parece de conexión (se reintenta solo). */
    data class Failed(val offline: Boolean) : SyncState
}
