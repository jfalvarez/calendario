package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.repository.PushTokenProvider
import com.jfaf.calendario.domain.repository.PushTokenRepository

/**
 * Asocia el token de avisos de este dispositivo a la cuenta con sesión. Sin proveedor de tokens
 * (escritorio) no hace nada. Si falla (sin red), se reintenta en el siguiente arranque.
 */
class RegisterPushTokenUseCase(
    private val tokensRemote: PushTokenRepository,
    private val tokens: PushTokenProvider?,
) {
    suspend operator fun invoke(token: String? = null) {
        val value = token ?: tokens?.currentToken() ?: return
        tokensRemote.registerPushToken(value)
    }
}

/**
 * Quita el token de este dispositivo al cerrar sesión, para que sus avisos no lleguen al siguiente usuario de este móvil.
 */
class UnregisterPushTokenUseCase(
    private val tokensRemote: PushTokenRepository,
    private val tokens: PushTokenProvider?,
) {
    suspend operator fun invoke() {
        val value = tokens?.currentToken() ?: return
        tokensRemote.unregisterPushToken(value)
    }
}
