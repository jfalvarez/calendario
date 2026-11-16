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
    val onSignIn: () -> Unit = {},
    val onSignUp: () -> Unit = {},
    val onSignOut: () -> Unit = {},
    val onSyncNow: () -> Unit = {},
)

/** Cuenta opcional + estado de la sincronización. Sin cuenta la app funciona sólo en este dispositivo. */
@Composable
fun AccountSection(state: AccountUiState, actions: AccountActions) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.account_title), style = MaterialTheme.typography.titleMedium)
        when (val auth = state.auth) {
            AuthState.Loading -> Text(stringResource(Res.string.account_loading))
            AuthState.SignedOut -> SignedOutForm(state, actions)
            is AuthState.SignedIn -> {
                Text(stringResource(Res.string.signed_in_as, auth.email ?: auth.userId))
                Text(syncText(state.sync), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = actions.onSyncNow, enabled = state.sync != SyncState.Syncing) {
                        Text(stringResource(Res.string.sync_now))
                    }
                    OutlinedButton(onClick = actions.onSignOut) { Text(stringResource(Res.string.sign_out)) }
                }
            }
        }
    }
}

@Composable
private fun SignedOutForm(state: AccountUiState, actions: AccountActions) {
    Text(stringResource(Res.string.account_hint), style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(
        state.email, actions.onEmailChange,
        label = { Text(stringResource(Res.string.email)) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        state.password, actions.onPasswordChange,
        label = { Text(stringResource(Res.string.password)) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
    state.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = actions.onSignIn, enabled = !state.busy) { Text(stringResource(Res.string.sign_in)) }
        OutlinedButton(onClick = actions.onSignUp, enabled = !state.busy) { Text(stringResource(Res.string.sign_up)) }
    }
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
}
