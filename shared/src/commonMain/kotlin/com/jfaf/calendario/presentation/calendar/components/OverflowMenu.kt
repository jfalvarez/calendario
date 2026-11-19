package com.jfaf.calendario.presentation.calendar.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jfaf.calendario.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun OverflowMenu(onOpenShifts: () -> Unit, onOpenStats: () -> Unit, onOpenDebts: () -> Unit, onOpenSettings: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, stringResource(Res.string.more_options))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.manage_shifts)) },
                onClick = { expanded = false; onOpenShifts() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.debts_title)) },
                onClick = { expanded = false; onOpenDebts() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.stats_title)) },
                onClick = { expanded = false; onOpenStats() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.settings_title)) },
                onClick = { expanded = false; onOpenSettings() },
            )
        }
    }
}
