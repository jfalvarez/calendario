package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.AuthError
import com.jfaf.calendario.domain.model.AuthState
import com.jfaf.calendario.domain.model.SyncState
import com.jfaf.calendario.domain.repository.AuthRepository
import com.jfaf.calendario.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

const val MIN_PASSWORD_LENGTH = 6

private fun validate(email: String, password: String): AuthError? = when {
    !email.trim().let { it.contains('@') && it.substringAfter('@').contains('.') && !it.contains(' ') } -> AuthError.InvalidEmail
    password.length < MIN_PASSWORD_LENGTH -> AuthError.WeakPassword
    else -> null
}

class SignInUseCase(private val auth: AuthRepository) {
    /** Valida los datos antes de llamar al servidor. Devuelve el error o `null`. */
    suspend operator fun invoke(email: String, password: String): AuthError? =
        if (validate(email, password) == AuthError.InvalidEmail) AuthError.InvalidEmail
        else auth.signIn(email.trim(), password)
}

class SignUpUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AuthError? =
        validate(email, password) ?: auth.signUp(email.trim(), password)
}

class SignOutUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke() = auth.signOut()
}

class ObserveAuthStateUseCase(private val auth: AuthRepository) {
    operator fun invoke(): Flow<AuthState> = auth.authState
}

class ObserveSyncStateUseCase(private val sync: SyncRepository) {
    operator fun invoke(): StateFlow<SyncState> = sync.state
}

class RequestSyncUseCase(private val sync: SyncRepository) {
    operator fun invoke() = sync.requestSync()
}
