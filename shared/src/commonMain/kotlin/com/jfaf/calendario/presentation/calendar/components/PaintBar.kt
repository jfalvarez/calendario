package com.jfaf.calendario.presentation.calendar.components

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.jfaf.calendario.presentation.calendar.*
import com.jfaf.calendario.presentation.common.color
import com.jfaf.calendario.presentation.common.onShiftColor
import com.jfaf.calendario.resources.*
import com.jfaf.calendario.resources.Res
import com.jfaf.calendario.resources.as_second_shift
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PaintBar(state: CalendarUiState, actions: CalendarActions) {
    Text(
        stringResource(Res.string.paint_mode_hint),
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 8.dp),
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(checked = state.paintAsSecond, onCheckedChange = actions.onPaintSecondChange)
        Text(stringResource(Res.string.as_second_shift), style = MaterialTheme.typography.labelMedium)
    }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.shifts.forEach { shift ->
            val selected = state.paintShiftId == shift.id
            val color = shift.color
            FilterChip(
                selected = selected,
                onClick = { actions.onShiftSelected(shift.id) },
                label = { Text("${shift.abbreviation} · ${shift.name}") },
                leadingIcon = if (selected) {
                    { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = color,
                    labelColor = color.onShiftColor,
                    selectedContainerColor = color,
                    selectedLabelColor = color.onShiftColor,
                    selectedLeadingIconColor = color.onShiftColor,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = color,
                    selectedBorderColor = MaterialTheme.colorScheme.onSurface,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 3.dp,
                ),
            )
        }
    }
}

@Composable
internal fun EditBar(state: CalendarUiState, actions: CalendarActions) {
    val hasSelection = state.selection.isNotEmpty()
    val haptics = LocalHapticFeedback.current
    Text(
        if (hasSelection) stringResource(Res.string.selected_count, state.selection.size)
        else stringResource(Res.string.edit_mode_hint),
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 8.dp),
    )
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        ActionButton(Icons.Filled.Brush, Res.string.assign_shift, hasSelection, actions.onAssignClick)
        ActionButton(Icons.Filled.ContentCopy, Res.string.copy, hasSelection, actions.onCopy)
        ActionButton(Icons.Filled.ContentCut, Res.string.cut, hasSelection, actions.onCut)
        ActionButton(Icons.Filled.ContentPaste, Res.string.paste, hasSelection && state.hasClipboard) {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            actions.onPaste()
        }
        ActionButton(Icons.Filled.Repeat, Res.string.repeat, hasSelection, actions.onRepeatClick)
        ActionButton(Icons.Filled.Flag, Res.string.local_holiday, hasSelection, actions.onToggleLocalHoliday)
        ActionButton(Icons.Filled.Delete, Res.string.delete, hasSelection, actions.onDelete)
        ActionButton(Icons.Filled.Close, Res.string.clear_selection, hasSelection, actions.onClearSelection)
    }
}

@Composable
internal fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: org.jetbrains.compose.resources.StringResource,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled) { Icon(icon, stringResource(label)) }
}
