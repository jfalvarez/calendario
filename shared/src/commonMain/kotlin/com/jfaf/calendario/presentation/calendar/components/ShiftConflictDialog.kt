package com.jfaf.calendario.presentation.calendar.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.jfaf.calendario.resources.Res
import com.jfaf.calendario.resources.accept
import com.jfaf.calendario.resources.shift_conflict_text
import com.jfaf.calendario.resources.shift_conflict_title
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/** Aviso de que el turno no se puede añadir como segundo: el día ya está lleno o se solapa en horas. */
@Composable
internal fun ShiftConflictDialog(dates: List<LocalDate>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.shift_conflict_title)) },
        text = {
            Text(stringResource(Res.string.shift_conflict_text, dates.joinToString { "${it.day}/${it.month.ordinal + 1}" }))
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.accept)) } },
    )
}
