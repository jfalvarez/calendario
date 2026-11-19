package com.jfaf.calendario.fakes

import com.jfaf.calendario.domain.model.AuthError
import com.jfaf.calendario.domain.model.AuthState
import com.jfaf.calendario.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Falso de [AuthRepository]. Los errores se configuran por operación; `null` = éxito. */
class FakeAuthRepository(initial: AuthState = AuthState.SignedOut) : AuthRepository {
    private val state = MutableStateFlow(initial)
    override val authState: StateFlow<AuthState> = state.asStateFlow()

    var signInError: AuthError? = null
    var signUpError: AuthError? = null
    var resetError: AuthError? = null
    var verifyError: AuthError? = null
    var updateError: AuthError? = null
    /** Operaciones llamadas, en orden (p. ej. "signIn:correo"). */
    val calls = mutableListOf<String>()

    override suspend fun signIn(email: String, password: String): AuthError? {
        calls += "signIn:$email"
        signInError?.let { return it }
        state.value = AuthState.SignedIn("user-1", email)
        return null
    }

    override suspend fun signUp(email: String, password: String): AuthError? {
        calls += "signUp:$email"
        return signUpError
    }

    override suspend fun signOut() {
        calls += "signOut"
        state.value = AuthState.SignedOut
    }

    override suspend fun sendPasswordReset(email: String): AuthError? {
        calls += "reset:$email"
        return resetError
    }

    override suspend fun verifyRecoveryCode(email: String, code: String): AuthError? {
        calls += "verify:$code"
        return verifyError
    }

    override suspend fun updatePassword(newPassword: String): AuthError? {
        calls += "update"
        return updateError
    }
}
