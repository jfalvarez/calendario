package com.jfaf.calendario.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.jfaf.calendario.domain.model.AuthError
import com.jfaf.calendario.domain.model.AuthState
import com.jfaf.calendario.domain.model.SyncState
import com.jfaf.calendario.presentation.stats.formatDate
import com.jfaf.calendario.resources.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

class AccountActions(
    val onEmailChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onCodeChange: (String) -> Unit = {},
    val onNewPasswordChange: (String) -> Unit = {},
    val onRepeatPasswordChange: (String) -> Unit = {},
    val onSignIn: () -> Unit = {},
    val onSignUp: () -> Unit = {},
    val onSignOut: () -> Unit = {},
    val onSyncNow: () -> Unit = {},
    val onForgotPassword: () -> Unit = {},
    val onBackToSignIn: () -> Unit = {},
    val onSendCode: () -> Unit = {},
    val onResetPassword: () -> Unit = {},
    val onStartChangePassword: () -> Unit = {},
    val onCancelChangePassword: () -> Unit = {},
    val onSaveNewPassword: () -> Unit = {},
)

/** Cuenta opcional + estado de la sincronización. Sin cuenta la app funciona sólo en este dispositivo. */
@Composable
fun AccountSection(state: AccountUiState, actions: AccountActions) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.account_title), style = MaterialTheme.typography.titleMedium)
        when {
            // Los formularios de recuperación tienen prioridad: al verificar el código el usuario ya tiene sesión,
            // pero debe seguir viendo el formulario hasta fijar la contraseña nueva.
            state.mode == AccountMode.ForgotEmail -> ForgotEmailForm(state, actions)
            state.mode == AccountMode.ForgotCode -> ForgotCodeForm(state, actions)
            else -> when (val auth = state.auth) {
                AuthState.Loading -> Text(stringResource(Res.string.account_loading))
                AuthState.SignedOut -> SignedOutForm(state, actions)
                is AuthState.SignedIn -> SignedInView(state, actions, auth)
            }
        }
    }
}

@Composable
private fun SignedOutForm(state: AccountUiState, actions: AccountActions) {
    Text(stringResource(Res.string.account_hint), style = MaterialTheme.typography.bodySmall)
    EmailField(state.email, actions.onEmailChange)
    PasswordField(state.password, actions.onPasswordChange, Res.string.password)
    state.error?.let { ErrorText(it) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = actions.onSignIn, enabled = !state.busy) { Text(stringResource(Res.string.sign_in)) }
        OutlinedButton(onClick = actions.onSignUp, enabled = !state.busy) { Text(stringResource(Res.string.sign_up)) }
    }
    TextButton(onClick = actions.onForgotPassword, enabled = !state.busy) {
        Text(stringResource(Res.string.forgot_password))
    }
}

@Composable
private fun ForgotEmailForm(state: AccountUiState, actions: AccountActions) {
    Text(stringResource(Res.string.forgot_hint), style = MaterialTheme.typography.bodySmall)
    EmailField(state.email, actions.onEmailChange)
    state.error?.let { ErrorText(it) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = actions.onSendCode, enabled = !state.busy) { Text(stringResource(Res.string.send_code)) }
        OutlinedButton(onClick = actions.onBackToSignIn, enabled = !state.busy) { Text(stringResource(Res.string.back)) }
    }
}

@Composable
private fun ForgotCodeForm(state: AccountUiState, actions: AccountActions) {
    Text(stringResource(Res.string.code_hint), style = MaterialTheme.typography.bodySmall)
    if (state.notice == AccountNotice.CodeSent) {
        Text(stringResource(Res.string.code_sent_notice), style = MaterialTheme.typography.bodySmall)
    }
    OutlinedTextField(
        state.code, actions.onCodeChange,
        label = { Text(stringResource(Res.string.code_label)) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    NewPasswordFields(state, actions)
    state.error?.let { ErrorText(it) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = actions.onResetPassword, enabled = !state.busy) { Text(stringResource(Res.string.reset_password)) }
        OutlinedButton(onClick = actions.onSendCode, enabled = !state.busy) { Text(stringResource(Res.string.resend_code)) }
    }
    OutlinedButton(onClick = actions.onBackToSignIn, enabled = !state.busy) { Text(stringResource(Res.string.back)) }
}

@Composable
private fun SignedInView(state: AccountUiState, actions: AccountActions, auth: AuthState.SignedIn) {
    Text(stringResource(Res.string.signed_in_as, auth.email ?: auth.userId))
    Text(syncText(state.sync), style = MaterialTheme.typography.bodyMedium)
    if (state.notice == AccountNotice.PasswordChanged) {
        Text(stringResource(Res.string.password_changed), style = MaterialTheme.typography.bodySmall)
    }
    if (state.mode == AccountMode.ChangePassword) {
        NewPasswordFields(state, actions)
        state.error?.let { ErrorText(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = actions.onSaveNewPassword, enabled = !state.busy) { Text(stringResource(Res.string.save)) }
            OutlinedButton(onClick = actions.onCancelChangePassword, enabled = !state.busy) {
                Text(stringResource(Res.string.cancel))
            }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = actions.onSyncNow, enabled = state.sync != SyncState.Syncing) {
                Text(stringResource(Res.string.sync_now))
            }
            OutlinedButton(onClick = actions.onSignOut) { Text(stringResource(Res.string.sign_out)) }
        }
        OutlinedButton(onClick = actions.onStartChangePassword) { Text(stringResource(Res.string.change_password)) }
    }
}

@Composable
private fun NewPasswordFields(state: AccountUiState, actions: AccountActions) {
    PasswordField(state.newPassword, actions.onNewPasswordChange, Res.string.new_password)
    PasswordField(state.repeatPassword, actions.onRepeatPasswordChange, Res.string.repeat_password)
}

@Composable
private fun EmailField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, onChange,
        label = { Text(stringResource(Res.string.email)) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: org.jetbrains.compose.resources.StringResource) {
    OutlinedTextField(
        value, onChange,
        label = { Text(stringResource(label)) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ErrorText(error: AuthError) {
    Text(stringResource(error.messageRes()), color = MaterialTheme.colorScheme.error)
}

@OptIn(ExperimentalTime::class)
@Composable
private fun syncText(sync: SyncState): String = when (sync) {
    SyncState.NotSignedIn -> ""
    SyncState.Syncing -> stringResource(Res.string.sync_syncing)
    is SyncState.Idle -> sync.lastSyncedAt?.let {
        val time = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault())
        val hhmm = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
        stringResource(Res.string.sync_idle, "${formatDate(time.date)} $hhmm")
    } ?: stringResource(Res.string.sync_pending)
    is SyncState.Failed -> stringResource(if (sync.offline) Res.string.sync_offline else Res.string.sync_failed)
}

private fun AuthError.messageRes() = when (this) {
    AuthError.InvalidEmail -> Res.string.auth_error_invalid_email
    AuthError.WeakPassword -> Res.string.auth_error_weak_password
    AuthError.InvalidCredentials -> Res.string.auth_error_invalid_credentials
    AuthError.EmailAlreadyUsed -> Res.string.auth_error_email_used
    AuthError.Network -> Res.string.auth_error_network
    AuthError.Unknown -> Res.string.auth_error_unknown
    AuthError.EmailConfirmationRequired -> Res.string.auth_error_confirm_email
    AuthError.InvalidCode -> Res.string.auth_error_invalid_code
    AuthError.PasswordMismatch -> Res.string.auth_error_password_mismatch
    AuthError.TooManyRequests -> Res.string.auth_error_too_many
}
