package com.jfaf.calendario.domain.repository

/** Token de avisos de cada dispositivo, asociado a la cuenta del servidor. */
interface PushTokenRepository {
    /** Asocia el token de avisos de este dispositivo a mi cuenta. */
    suspend fun registerPushToken(token: String): Result<Unit>

    /** Quita el token de avisos de este dispositivo (al cerrar sesión). */
    suspend fun unregisterPushToken(token: String): Result<Unit>
}
