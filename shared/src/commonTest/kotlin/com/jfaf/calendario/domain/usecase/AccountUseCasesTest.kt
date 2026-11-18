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
        override suspend fun sendPasswordReset(email: String): AuthError? { calls += "reset:$email"; return null }
        override suspend fun verifyRecoveryCode(email: String, code: String): AuthError? { calls += "otp:$email:$code"; return null }
        override suspend fun updatePassword(newPassword: String): AuthError? { calls += "pwd"; return null }
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

    @Test
    fun resetCodeIsValidatedBeforeCallingTheServer() = runTest {
        val complete = CompletePasswordResetUseCase(auth)
        assertEquals(AuthError.InvalidEmail, complete("nope", "123456", "nueva1", "nueva1"))
        assertEquals(AuthError.InvalidCode, complete("a@b.com", "12345", "nueva1", "nueva1"))
        assertEquals(AuthError.InvalidCode, complete("a@b.com", "12a456", "nueva1", "nueva1"))
        assertEquals(AuthError.WeakPassword, complete("a@b.com", "123456", "123", "123"))
        assertEquals(AuthError.PasswordMismatch, complete("a@b.com", "123456", "nueva1", "nueva2"))
        assertEquals(emptyList(), auth.calls)
        assertNull(complete(" a@b.com ", " 123456 ", "nueva1", "nueva1"))
        assertEquals(listOf("otp:a@b.com:123456", "pwd"), auth.calls)
    }

    @Test
    fun resetRequestAndPasswordChangeValidateFirst() = runTest {
        assertEquals(AuthError.InvalidEmail, RequestPasswordResetUseCase(auth)("sin-arroba"))
        assertEquals(emptyList(), auth.calls)
        assertNull(RequestPasswordResetUseCase(auth)(" a@b.com "))
        assertEquals(listOf("reset:a@b.com"), auth.calls)

        assertEquals(AuthError.WeakPassword, ChangePasswordUseCase(auth)("123", "123"))
        assertEquals(AuthError.PasswordMismatch, ChangePasswordUseCase(auth)("123456", "123457"))
        assertEquals(listOf("reset:a@b.com"), auth.calls)
        assertNull(ChangePasswordUseCase(auth)("123456", "123456"))
        assertEquals(listOf("reset:a@b.com", "pwd"), auth.calls)
    }
}
