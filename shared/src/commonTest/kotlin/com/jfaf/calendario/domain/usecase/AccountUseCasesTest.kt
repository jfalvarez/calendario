package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.AuthError
import com.jfaf.calendario.domain.model.AuthState
import com.jfaf.calendario.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountUseCasesTest {
    private class FakeAuth : AuthRepository {
        val calls = mutableListOf<String>()
        override val authState: Flow<AuthState> = MutableStateFlow(AuthState.SignedOut)
        override suspend fun signIn(email: String, password: String): AuthError? { calls += "in:$email"; return null }
        override suspend fun signUp(email: String, password: String): AuthError? { calls += "up:$email"; return null }
        override suspend fun signOut() { calls += "out" }
    }

    private val auth = FakeAuth()

    @Test
    fun signUpValidatesBeforeCallingTheServer() = runTest {
        val signUp = SignUpUseCase(auth)
        assertEquals(AuthError.InvalidEmail, signUp("sin-arroba", "123456"))
        assertEquals(AuthError.InvalidEmail, signUp("a@b", "123456"))
        assertEquals(AuthError.WeakPassword, signUp("a@b.com", "12345"))
        assertEquals(emptyList(), auth.calls)
        assertNull(signUp("  a@b.com ", "123456"))
        assertEquals(listOf("up:a@b.com"), auth.calls) // el correo se recorta
    }

    @Test
    fun signInOnlyChecksTheEmailShape() = runTest {
        val signIn = SignInUseCase(auth)
        assertEquals(AuthError.InvalidEmail, signIn("nope", "x"))
        assertNull(signIn("a@b.com", "abc")) // una contraseña corta se prueba igualmente en el servidor
        assertEquals(listOf("in:a@b.com"), auth.calls)
    }
}
