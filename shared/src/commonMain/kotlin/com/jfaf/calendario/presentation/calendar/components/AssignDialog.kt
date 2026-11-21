package com.jfaf.calendario.presentation.calendar.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.presentation.calendar.*
import com.jfaf.calendario.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AssignDialog(
    shifts: List<Shift>,
    onDismiss: () -> Unit,
    onAssign: (String, Boolean) -> Unit,
) {
    var asSecond by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.assign_shift)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = asSecond, onCheckedChange = { asSecond = it })
                    Text(stringResource(Res.string.as_second_shift))
                }
                shifts.forEach { shift ->
                    Row(
                        Modifier.fillMaxWidth()
                            .combinedClickable(onClick = { onAssign(shift.id, asSecond) })
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ShiftTag(shift, Modifier.weight(0.25f))
                        Text(shift.name, Modifier.weight(0.75f))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}
