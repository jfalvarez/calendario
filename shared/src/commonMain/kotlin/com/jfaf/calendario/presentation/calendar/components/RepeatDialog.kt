package com.jfaf.calendario.presentation.calendar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jfaf.calendario.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RepeatDialog(onDismiss: () -> Unit, onRepeat: (Int) -> Unit) {
    var text by remember { mutableStateOf("1") }
    val times = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.repeat_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.repeat_hint))
                OutlinedTextField(
                    text, { text = it.filter(Char::isDigit).take(3) },
                    label = { Text(stringResource(Res.string.repeat_times)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { times?.let(onRepeat) }, enabled = times != null && times > 0) {
                Text(stringResource(Res.string.apply))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}
