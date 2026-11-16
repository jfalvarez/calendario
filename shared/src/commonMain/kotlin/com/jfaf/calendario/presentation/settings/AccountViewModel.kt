package com.jfaf.calendario.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.model.AuthError
import com.jfaf.calendario.domain.model.AuthState
import com.jfaf.calendario.domain.model.SyncState
import com.jfaf.calendario.domain.usecase.ObserveAuthStateUseCase
import com.jfaf.calendario.domain.usecase.ObserveSyncStateUseCase
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

data class AccountUiState(
    val auth: AuthState = AuthState.Loading,
    val sync: SyncState = SyncState.NotSignedIn,
    val email: String = "",
    val password: String = "",
    val busy: Boolean = false,
    val error: AuthError? = null,
)

class AccountViewModel(
    observeAuth: ObserveAuthStateUseCase,
    observeSync: ObserveSyncStateUseCase,
    private val signIn: SignInUseCase,
    private val signUp: SignUpUseCase,
    private val signOut: SignOutUseCase,
    private val requestSync: RequestSyncUseCase,
) : ViewModel() {
    private data class Form(val email: String = "", val password: String = "", val busy: Boolean = false, val error: AuthError? = null)

    private val form = MutableStateFlow(Form())

    val uiState: StateFlow<AccountUiState> = combine(observeAuth(), observeSync(), form) { auth, sync, f ->
        AccountUiState(auth, sync, f.email, f.password, f.busy, f.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountUiState())

    fun onEmailChange(value: String) = form.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = form.update { it.copy(password = value, error = null) }

    fun onSignIn() = submit { email, password -> signIn(email, password) }
    fun onSignUp() = submit { email, password -> signUp(email, password) }

    private fun submit(action: suspend (String, String) -> AuthError?) {
        val current = form.value
        if (current.busy) return
        form.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val error = action(current.email, current.password)
            // Con éxito se limpia el formulario (la contraseña no debe quedar en memoria de la pantalla).
            form.value = if (error == null) Form() else current.copy(busy = false, error = error)
        }
    }

    fun onSignOut() {
        viewModelScope.launch { signOut() }
    }

    fun onSyncNow() = requestSync()
}
