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
}
