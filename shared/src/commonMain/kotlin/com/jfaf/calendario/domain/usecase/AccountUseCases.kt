package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.AuthError
import com.jfaf.calendario.domain.model.AuthState
import com.jfaf.calendario.domain.model.SyncState
import com.jfaf.calendario.domain.repository.AuthRepository
import com.jfaf.calendario.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

const val MIN_PASSWORD_LENGTH = 6
const val RECOVERY_CODE_LENGTH = 6

private fun isValidEmail(email: String): Boolean =
    email.trim().let { it.contains('@') && it.substringAfter('@').contains('.') && !it.contains(' ') }

private fun validate(email: String, password: String): AuthError? = when {
    !isValidEmail(email) -> AuthError.InvalidEmail
    password.length < MIN_PASSWORD_LENGTH -> AuthError.WeakPassword
    else -> null
}

/** Validación de una contraseña nueva escrita dos veces (cambio o recuperación). */
private fun validateNewPassword(newPassword: String, repeated: String): AuthError? = when {
    newPassword.length < MIN_PASSWORD_LENGTH -> AuthError.WeakPassword
    newPassword != repeated -> AuthError.PasswordMismatch
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

/** Pide el código de recuperación. Por privacidad, la respuesta no revela si el correo tiene cuenta. */
class RequestPasswordResetUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(email: String): AuthError? =
        if (!isValidEmail(email)) AuthError.InvalidEmail
        else auth.sendPasswordReset(email.trim())
}

/** Verifica el código y fija la contraseña nueva. Valida todo antes de llamar al servidor. */
class CompletePasswordResetUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(email: String, code: String, newPassword: String, repeated: String): AuthError? {
        if (!isValidEmail(email)) return AuthError.InvalidEmail
        if (code.trim().length != RECOVERY_CODE_LENGTH || !code.trim().all { it.isDigit() }) return AuthError.InvalidCode
        validateNewPassword(newPassword, repeated)?.let { return it }
        auth.verifyRecoveryCode(email.trim(), code.trim())?.let { return it }
        return auth.updatePassword(newPassword)
    }
}

/** Cambia la contraseña con la sesión ya iniciada. */
class ChangePasswordUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(newPassword: String, repeated: String): AuthError? =
        validateNewPassword(newPassword, repeated) ?: auth.updatePassword(newPassword)
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
