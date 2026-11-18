package com.jfaf.calendario.domain.repository

import com.jfaf.calendario.domain.model.AuthError
import com.jfaf.calendario.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthState>
    /** Devuelve el error, o `null` si se inició sesión. */
    suspend fun signIn(email: String, password: String): AuthError?
    suspend fun signUp(email: String, password: String): AuthError?
    suspend fun signOut()

    /** Envía al correo un código de un solo uso para restablecer la contraseña. */
    suspend fun sendPasswordReset(email: String): AuthError?
    /** Verifica el código recibido; si es correcto, deja la sesión iniciada para poder cambiar la contraseña. */
    suspend fun verifyRecoveryCode(email: String, code: String): AuthError?
    /** Cambia la contraseña de la sesión iniciada. */
    suspend fun updatePassword(newPassword: String): AuthError?
}
