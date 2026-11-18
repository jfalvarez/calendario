package com.jfaf.calendario.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.model.AuthError
import com.jfaf.calendario.domain.model.AuthState
import com.jfaf.calendario.domain.model.SyncState
import com.jfaf.calendario.domain.usecase.ChangePasswordUseCase
import com.jfaf.calendario.domain.usecase.CompletePasswordResetUseCase
import com.jfaf.calendario.domain.usecase.ObserveAuthStateUseCase
import com.jfaf.calendario.domain.usecase.ObserveSyncStateUseCase
import com.jfaf.calendario.domain.usecase.RequestPasswordResetUseCase
import com.jfaf.calendario.domain.usecase.RequestSyncUseCase
import com.jfaf.calendario.domain.usecase.SignInUseCase
import com.jfaf.calendario.domain.usecase.SignOutUseCase
import com.jfaf.calendario.domain.usecase.SignUpUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Qué formulario se muestra en la sección de cuenta. */
enum class AccountMode {
    /** Entrar o crear cuenta (sin sesión), o nada especial (con sesión). */
    SignIn,
    /** Pedir el código de recuperación por correo. */
    ForgotEmail,
    /** Escribir el código recibido y la contraseña nueva. */
    ForgotCode,
    /** Cambiar la contraseña con la sesión iniciada. */
    ChangePassword,
}

/** Mensajes informativos (no son errores). */
enum class AccountNotice { CodeSent, PasswordChanged }

data class AccountUiState(
    val auth: AuthState = AuthState.Loading,
    val sync: SyncState = SyncState.NotSignedIn,
    val mode: AccountMode = AccountMode.SignIn,
    val email: String = "",
    val password: String = "",
    val code: String = "",
    val newPassword: String = "",
    val repeatPassword: String = "",
    val busy: Boolean = false,
    val error: AuthError? = null,
    val notice: AccountNotice? = null,
)

class AccountViewModel(
    observeAuth: ObserveAuthStateUseCase,
    observeSync: ObserveSyncStateUseCase,
    private val signIn: SignInUseCase,
    private val signUp: SignUpUseCase,
    private val signOut: SignOutUseCase,
    private val requestSync: RequestSyncUseCase,
    private val requestPasswordReset: RequestPasswordResetUseCase,
    private val completePasswordReset: CompletePasswordResetUseCase,
    private val changePassword: ChangePasswordUseCase,
) : ViewModel() {
    private data class Form(
        val mode: AccountMode = AccountMode.SignIn,
        val email: String = "",
        val password: String = "",
        val code: String = "",
        val newPassword: String = "",
        val repeatPassword: String = "",
        val busy: Boolean = false,
        val error: AuthError? = null,
        val notice: AccountNotice? = null,
    )

    private val form = MutableStateFlow(Form())

    val uiState: StateFlow<AccountUiState> = combine(observeAuth(), observeSync(), form) { auth, sync, f ->
        AccountUiState(
            auth, sync, f.mode, f.email, f.password, f.code, f.newPassword, f.repeatPassword,
            f.busy, f.error, f.notice,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountUiState())

    fun onEmailChange(value: String) = form.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = form.update { it.copy(password = value, error = null) }
    fun onCodeChange(value: String) = form.update { it.copy(code = value, error = null) }
    fun onNewPasswordChange(value: String) = form.update { it.copy(newPassword = value, error = null) }
    fun onRepeatPasswordChange(value: String) = form.update { it.copy(repeatPassword = value, error = null) }

    fun onSignIn() = submit({ f -> signIn(f.email, f.password) })
    fun onSignUp() = submit({ f -> signUp(f.email, f.password) })

    /** Abre el formulario para pedir el código de recuperación. */
    fun onForgotPassword() = form.update { Form(mode = AccountMode.ForgotEmail, email = it.email) }

    /** Vuelve al inicio de sesión descartando lo escrito en el formulario de recuperación. */
    fun onBackToSignIn() = form.update { Form(email = it.email) }

    /** Envía (o reenvía) el código al correo. Avanza al paso de introducir el código. */
    fun onSendCode() = submit(
        action = { f -> requestPasswordReset(f.email) },
        next = { f ->
            f.copy(
                mode = AccountMode.ForgotCode, notice = AccountNotice.CodeSent, busy = false, error = null,
                code = "", newPassword = "", repeatPassword = "",
            )
        },
    )

    /** Verifica el código y fija la nueva contraseña. */
    fun onResetPassword() = submit(
        action = { f -> completePasswordReset(f.email, f.code, f.newPassword, f.repeatPassword) },
        next = { Form(notice = AccountNotice.PasswordChanged) },
    )

    fun onStartChangePassword() = form.update { Form(mode = AccountMode.ChangePassword) }
    fun onCancelChangePassword() = form.update { Form() }

    fun onSaveNewPassword() = submit(
        action = { f -> changePassword(f.newPassword, f.repeatPassword) },
        next = { Form(notice = AccountNotice.PasswordChanged) },
    )

    /**
     * Ejecuta una acción con el formulario actual. Con éxito pasa al estado que indique [next]
     * (por defecto, el formulario vacío): así la contraseña no queda en memoria de la pantalla.
     */
    private fun submit(
        action: suspend (Form) -> AuthError?,
        next: (Form) -> Form = { Form() },
    ) {
        val current = form.value
        if (current.busy) return
        form.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val error = action(current)
            form.value = if (error == null) next(current) else current.copy(busy = false, error = error)
        }
    }

    fun onSignOut() {
        viewModelScope.launch { signOut() }
    }

    fun onSyncNow() = requestSync()
}
