package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.repository.PushTokenProvider
import com.jfaf.calendario.domain.repository.PushTokenRepository

/**
 * Asocia el token de avisos de este dispositivo a la cuenta con sesión, y lo quita al cerrarla
 * para que los avisos de un usuario no lleguen al siguiente que entre en este móvil.
 * Sin proveedor de tokens (escritorio) no hace nada.
 */
class PushRegistration(
    private val tokensRemote: PushTokenRepository,
    private val tokens: PushTokenProvider?,
) {
    suspend fun register(token: String? = null) {
        val value = token ?: tokens?.currentToken() ?: return
        tokensRemote.registerPushToken(value) // si falla (sin red), se reintenta en el siguiente arranque
    }

    suspend fun unregister() {
        val value = tokens?.currentToken() ?: return
        tokensRemote.unregisterPushToken(value)
    }
}
