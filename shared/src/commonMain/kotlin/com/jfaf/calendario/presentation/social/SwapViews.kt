package com.jfaf.calendario.presentation.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jfaf.calendario.domain.model.LinkDirection
import com.jfaf.calendario.domain.model.MyShiftOption
import com.jfaf.calendario.domain.model.SwapDayPreview
import com.jfaf.calendario.domain.model.SwapProposal
import com.jfaf.calendario.domain.model.SwapStatus
import com.jfaf.calendario.domain.model.previewForMe
import com.jfaf.calendario.domain.repository.SocialError
import com.jfaf.calendario.resources.*
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/** Fecha corta "dd/mm". */
internal fun LocalDate.short(): String =
    "${day.toString().padStart(2, '0')}/${(month.ordinal + 1).toString().padStart(2, '0')}"

/** Antes y después de mi calendario para cada día que cambia. Lo que se ve aquí es lo que pasaría al aceptar. */
@Composable
fun SwapPreviewCard(previews: List<SwapDayPreview>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(Res.string.swap_preview_title), style = MaterialTheme.typography.labelLarge)
            previews.forEach { p ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${p.date.short()} · ${p.slotLabel()}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(Res.string.swap_before_after, p.before ?: stringResource(Res.string.swap_free), p.after ?: stringResource(Res.string.swap_free)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun SwapDayPreview.slotLabel(): String = if (slot == 0) "1.º" else "2.º"

/** Diálogo para elegir qué turno mío entrego a cambio del del compañero, con la vista previa antes de enviar. */
@Composable
fun ProposeSwapDialog(
    draft: ProposalDraft,
    onSelect: (MyShiftOption) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.swap_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(Res.string.swap_pick, draft.target.date.short(), draft.target.shiftName),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (draft.options.isEmpty()) {
                    Text(stringResource(Res.string.swap_no_options))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    draft.options.forEach { option ->
                        Row(
                            Modifier.fillMaxWidth().selectable(selected = draft.selected == option, onClick = { onSelect(option) }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = draft.selected == option, onClick = { onSelect(option) })
                            Text("${option.date.short()} · ${option.shiftName}")
                        }
                    }
                }
                if (draft.selected != null) SwapPreviewCard(draft.preview)
                draft.error?.let { Text(stringResource(swapErrorText(it)), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = onSend, enabled = draft.selected != null && !draft.sending) {
                Text(stringResource(Res.string.swap_send))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

/** Propuestas de cambio: las que me han hecho (aceptar o rechazar) y las mías pendientes (cancelar). */
@Composable
fun SwapList(
    swaps: List<SwapProposal>,
    busy: Boolean,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onCancel: (String) -> Unit,
) {
    if (swaps.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.swap_list_title), style = MaterialTheme.typography.titleSmall)
        swaps.forEach { swap ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(
                        if (swap.direction == LinkDirection.Incoming) Res.string.swap_incoming_from else Res.string.swap_outgoing_to,
                        swap.otherAlias.orEmpty(),
                    ) + " · " + stringResource(swapStatusText(swap.status)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                SwapPreviewCard(swap.previewForMe())
                if (swap.status == SwapStatus.Pending) {
                    if (swap.direction == LinkDirection.Incoming) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onAccept(swap.id) }, enabled = !busy) { Text(stringResource(Res.string.swap_accept)) }
                            OutlinedButton(onClick = { onReject(swap.id) }, enabled = !busy) { Text(stringResource(Res.string.swap_reject)) }
                        }
                    } else {
                        OutlinedButton(onClick = { onCancel(swap.id) }, enabled = !busy) { Text(stringResource(Res.string.swap_cancel)) }
                    }
                }
            }
        }
    }
}

private fun swapStatusText(status: SwapStatus) = when (status) {
    SwapStatus.Pending -> Res.string.swap_status_pending
    SwapStatus.Accepted -> Res.string.swap_status_accepted
    SwapStatus.Rejected -> Res.string.swap_status_rejected
    SwapStatus.Cancelled -> Res.string.swap_status_cancelled
}

private fun swapErrorText(error: SocialError) = when (error) {
    SocialError.ShiftMissing -> Res.string.swap_err_shift_missing
    SocialError.AssignmentMissing, SocialError.AssignmentChanged -> Res.string.swap_err_assignment
    SocialError.TargetBusy -> Res.string.swap_err_target_busy
    SocialError.ProposerBusy -> Res.string.swap_err_proposer_busy
    SocialError.LinkRevoked -> Res.string.swap_err_link
    else -> Res.string.social_error
}
