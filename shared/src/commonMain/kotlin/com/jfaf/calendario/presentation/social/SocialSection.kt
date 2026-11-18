package com.jfaf.calendario.presentation.social

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jfaf.calendario.domain.model.LinkDirection
import com.jfaf.calendario.domain.model.LinkRequestResult
import com.jfaf.calendario.domain.model.LinkStatus
import com.jfaf.calendario.domain.repository.SocialError
import com.jfaf.calendario.resources.*
import org.jetbrains.compose.resources.stringResource

class SocialActions(
    val onSaveAlias: (String) -> Unit = {},
    val onRequestLink: (String) -> Unit = {},
    val onInputChanged: () -> Unit = {},
    val onAccept: (String) -> Unit = {},
    val onReject: (String) -> Unit = {},
    val onBlock: (String) -> Unit = {},
    val onRevoke: (String) -> Unit = {},
    val onOpenShared: (ownerId: String, alias: String) -> Unit = { _, _ -> },
    val onRespondSwap: (swapId: String, accept: Boolean) -> Unit = { _, _ -> },
    val onCancelSwap: (String) -> Unit = {},
    val onUnblock: (String) -> Unit = {},
)

/** Sección de Ajustes: mi alias, pedir un vínculo, solicitudes pendientes y vínculos actuales. */
@Composable
fun SocialSection(state: SocialUiState, signedIn: Boolean, actions: SocialActions) {
    var aliasInput by rememberSaveable { mutableStateOf("") }
    var requestInput by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.social_section_title), style = MaterialTheme.typography.titleMedium)
        if (!signedIn) {
            Text(stringResource(Res.string.social_need_session), style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        // El alias se fija una sola vez: si ya existe, sólo se muestra (el servidor también lo impide).
        val myAlias = state.myAlias
        if (myAlias != null) {
            Text(stringResource(Res.string.social_alias_current, myAlias), style = MaterialTheme.typography.bodyLarge)
        } else {
            OutlinedTextField(
                value = aliasInput,
                onValueChange = { aliasInput = it; actions.onInputChanged() },
                label = { Text(stringResource(Res.string.social_alias_label)) },
                supportingText = { Text(stringResource(Res.string.social_alias_hint)) },
                singleLine = true,
                isError = state.invalidAliasInput,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { actions.onSaveAlias(aliasInput) }, enabled = !state.busy) {
                Text(stringResource(Res.string.social_alias_save))
            }
        }
        if (state.aliasSaved) Text(stringResource(Res.string.social_alias_saved), style = MaterialTheme.typography.bodySmall)

        OutlinedTextField(
            value = requestInput,
            onValueChange = { requestInput = it; actions.onInputChanged() },
            label = { Text(stringResource(Res.string.social_request_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { actions.onRequestLink(requestInput) }, enabled = !state.busy) {
            Text(stringResource(Res.string.social_request_send))
        }
        state.requestResult?.let { Text(stringResource(requestResultText(it)), style = MaterialTheme.typography.bodySmall) }

        if (state.pending.isNotEmpty()) {
            Text(stringResource(Res.string.social_pending_title), style = MaterialTheme.typography.titleSmall)
            state.pending.forEach { request ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(Res.string.social_pending_from, request.requesterAlias))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { actions.onAccept(request.linkId) }, enabled = !state.busy) {
                            Text(stringResource(Res.string.social_accept))
                        }
                        OutlinedButton(onClick = { actions.onReject(request.linkId) }, enabled = !state.busy) {
                            Text(stringResource(Res.string.social_reject))
                        }
                        TextButton(onClick = { actions.onBlock(request.linkId) }, enabled = !state.busy) {
                            Text(stringResource(Res.string.social_block))
                        }
                    }
                }
            }
        }

        if (state.links.isNotEmpty()) {
            Text(stringResource(Res.string.social_links_title), style = MaterialTheme.typography.titleSmall)
            state.links.forEach { link ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(link.otherAlias ?: "—")
                        Text(
                            stringResource(directionText(link.direction)) + " · " + stringResource(statusText(link.status)),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (link.direction == LinkDirection.Outgoing && link.status == LinkStatus.Accepted) {
                            TextButton(onClick = { actions.onOpenShared(link.otherId, link.otherAlias.orEmpty()) }) {
                                Text(stringResource(Res.string.social_view_calendar))
                            }
                        }
                        if (link.status == LinkStatus.Accepted || link.status == LinkStatus.Pending) {
                            TextButton(onClick = { actions.onRevoke(link.linkId) }, enabled = !state.busy) {
                                Text(stringResource(Res.string.social_revoke))
                            }
                        }
                    }
                }
            }
        }

        if (state.blocked.isNotEmpty()) {
            Text(stringResource(Res.string.social_blocked_title), style = MaterialTheme.typography.titleSmall)
            state.blocked.forEach { blocked ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(blocked.alias ?: "—")
                    TextButton(onClick = { actions.onUnblock(blocked.id) }, enabled = !state.busy) {
                        Text(stringResource(Res.string.social_unblock))
                    }
                }
            }
        }

        SwapList(
            swaps = state.swaps,
            busy = state.busy,
            onAccept = { actions.onRespondSwap(it, true) },
            onReject = { actions.onRespondSwap(it, false) },
            onCancel = actions.onCancelSwap,
        )

        state.error?.let { Text(stringResource(errorText(it)), color = MaterialTheme.colorScheme.error) }
    }
}

internal fun requestResultText(result: LinkRequestResult) = when (result) {
    LinkRequestResult.Sent -> Res.string.social_request_sent
    LinkRequestResult.AliasNotFound -> Res.string.social_request_not_found
    LinkRequestResult.Self -> Res.string.social_request_self
    LinkRequestResult.TooManyAttempts -> Res.string.social_request_too_many_attempts
    LinkRequestResult.TooManyPending -> Res.string.social_request_too_many_pending
}

internal fun errorText(error: SocialError) = when (error) {
    SocialError.AliasTaken -> Res.string.social_alias_taken
    SocialError.InvalidAlias -> Res.string.social_invalid_alias
    SocialError.AliasLocked -> Res.string.social_alias_locked
    else -> Res.string.social_error
}

private fun directionText(direction: LinkDirection) =
    if (direction == LinkDirection.Incoming) Res.string.social_incoming else Res.string.social_outgoing

private fun statusText(status: LinkStatus) = when (status) {
    LinkStatus.Pending -> Res.string.social_pending_status
    LinkStatus.Accepted -> Res.string.social_accepted_status
    LinkStatus.Rejected -> Res.string.social_rejected_status
    LinkStatus.Revoked -> Res.string.social_revoked_status
}
