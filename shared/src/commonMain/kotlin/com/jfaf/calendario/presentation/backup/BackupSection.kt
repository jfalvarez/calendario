package com.jfaf.calendario.presentation.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.jfaf.calendario.domain.platform.BackupFileIo
import com.jfaf.calendario.resources.*
import org.jetbrains.compose.resources.stringResource

/** Ajustes: exportar la copia, o importar una y confirmar la sustitución de los datos actuales. */
@Composable
fun BackupSection(state: BackupUiState, io: BackupFileIo?, onExport: () -> Unit, onImport: () -> Unit) {
    if (io == null) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.backup_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.backup_hint), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onExport, enabled = !state.busy) { Text(stringResource(Res.string.backup_export)) }
        OutlinedButton(onClick = onImport, enabled = !state.busy) { Text(stringResource(Res.string.backup_import)) }
        if (state.busy) Text(stringResource(Res.string.backup_busy), style = MaterialTheme.typography.bodySmall)
        state.notice?.let { Text(stringResource(noticeText(it)), style = MaterialTheme.typography.bodySmall) }
    }
}

/** Diálogo de confirmación: la importación borra lo actual, así que siempre pregunta antes. */
@Composable
fun BackupConfirmDialog(pending: String?, onConfirm: () -> Unit, onCancel: () -> Unit) {
    if (pending == null) return
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(Res.string.backup_confirm_title)) },
        text = { Text(stringResource(Res.string.backup_confirm_body)) },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(Res.string.backup_confirm)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(Res.string.cancel)) } },
    )
}

private fun noticeText(notice: BackupNotice) = when (notice) {
    BackupNotice.Exported -> Res.string.backup_exported
    BackupNotice.Imported -> Res.string.backup_imported
    BackupNotice.InvalidFile -> Res.string.backup_invalid
    BackupNotice.Failed -> Res.string.backup_failed
}
