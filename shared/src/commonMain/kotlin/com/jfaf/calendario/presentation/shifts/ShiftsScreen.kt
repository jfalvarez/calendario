package com.jfaf.calendario.presentation.shifts

import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import com.jfaf.calendario.domain.model.AppStyle
import com.jfaf.calendario.presentation.theme.LocalAppStyle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.presentation.common.color
import com.jfaf.calendario.presentation.common.onShiftColor
import com.jfaf.calendario.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ShiftsScreen(onBack: () -> Unit, viewModel: ShiftsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    ShiftsContent(
        state = state,
        onBack = onBack,
        onAdd = viewModel::onAdd,
        onEdit = viewModel::onEdit,
        onMove = viewModel::onMove,
        onRequestDelete = viewModel::onRequestDelete,
        onEditorChange = viewModel::onEditorChange,
        onDismissEditor = viewModel::onDismissEditor,
        onSave = viewModel::onSave,
        onConfirmDelete = viewModel::onConfirmDelete,
        onCancelDelete = viewModel::onCancelDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftsContent(
    state: ShiftsUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Shift) -> Unit,
    onMove: (String, Int) -> Unit,
    onRequestDelete: (Shift) -> Unit,
    onEditorChange: ((ShiftEditorState) -> ShiftEditorState) -> Unit,
    onDismissEditor: () -> Unit,
    onSave: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.shifts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, shape = MaterialTheme.shapes.large) {
                Icon(Icons.Filled.Add, stringResource(Res.string.add_shift))
            }
        },
    ) { padding ->
        if (state.shifts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.shifts_empty))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                itemsIndexed(state.shifts, key = { _, s -> s.id }) { index, shift ->
                    ShiftRow(
                        shift = shift,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.shifts.lastIndex,
                        onClick = { onEdit(shift) },
                        onMove = { onMove(shift.id, it) },
                        onDelete = { onRequestDelete(shift) },
                    )
                }
            }
        }
    }

    state.editor?.let { ShiftEditorDialog(it, onEditorChange, onDismissEditor, onSave) }
    state.pendingDelete?.let { shift ->
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text(stringResource(Res.string.delete_shift_title)) },
            text = { Text("${shift.name}. ${stringResource(Res.string.delete_shift_message)}") },
            confirmButton = { TextButton(onClick = onConfirmDelete) { Text(stringResource(Res.string.delete)) } },
            dismissButton = { TextButton(onClick = onCancelDelete) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun ShiftRow(
    shift: Shift,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    val style = LocalAppStyle.current
    val colors = MaterialTheme.colorScheme
    // Pop: tarjeta con el color del turno. Sorbete: tarjeta tonal. Neón: fila plana sobre el fondo.
    val container = when (style) {
        AppStyle.Pop -> shift.color
        AppStyle.Sorbete -> colors.surfaceContainerLow
        AppStyle.Neon -> Color.Transparent
    }
    val fg = if (style == AppStyle.Pop) shift.color.onShiftColor else colors.onSurface
    val iconColors = IconButtonDefaults.iconButtonColors(contentColor = fg, disabledContentColor = fg.copy(alpha = 0.38f))
    Surface(
        Modifier.fillMaxWidth().padding(vertical = if (style == AppStyle.Neon) 2.dp else 4.dp),
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = fg,
    ) {
        Row(
            Modifier.clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val badgeBg = if (style == AppStyle.Pop) colors.surface else shift.color
            val badgeFg = if (style == AppStyle.Pop) colors.onSurface else shift.color.onShiftColor
            Box(
                Modifier.size(46.dp).background(badgeBg, if (style == AppStyle.Sorbete) MaterialTheme.shapes.medium else CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(shift.abbreviation, color = badgeFg, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
            Column(Modifier.weight(1f)) {
                Text(shift.name, style = MaterialTheme.typography.titleMedium)
                scheduleText(shift)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            IconButton(onClick = { onMove(-1) }, enabled = canMoveUp, colors = iconColors) {
                Icon(Icons.Filled.KeyboardArrowUp, stringResource(Res.string.move_up))
            }
            IconButton(onClick = { onMove(1) }, enabled = canMoveDown, colors = iconColors) {
                Icon(Icons.Filled.KeyboardArrowDown, stringResource(Res.string.move_down))
            }
            IconButton(onClick = onDelete, colors = iconColors) { Icon(Icons.Filled.Delete, stringResource(Res.string.delete)) }
        }
    }
}

@Composable
private fun scheduleText(shift: Shift): String? {
    val start = shift.startMinutes ?: return null
    val end = shift.endMinutes ?: return null
    val work = shift.workMinutes ?: return null
    return "${formatMinutes(start)}–${formatMinutes(end)} · " +
        stringResource(Res.string.shift_duration, work / 60, (work % 60).toString().padStart(2, '0'))
}

@Composable
private fun ShiftEditorDialog(
    editor: ShiftEditorState,
    onChange: ((ShiftEditorState) -> ShiftEditorState) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (editor.isNew) Res.string.new_shift else Res.string.edit_shift))
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    editor.name, { v -> onChange { it.copy(name = v) } },
                    label = { Text(stringResource(Res.string.name)) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    editor.abbreviation, { v -> onChange { it.copy(abbreviation = v) } },
                    label = { Text(stringResource(Res.string.abbreviation)) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(Res.string.color), style = MaterialTheme.typography.labelMedium)
                ShiftColors.chunked(6).forEach { rowColors ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowColors.forEach { color ->
                            val selected = color == editor.colorArgb
                            Box(
                                Modifier.size(32.dp)
                                    .background(Color(color), CircleShape)
                                    .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    .clickable { onChange { it.copy(colorArgb = color) } },
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        editor.start, { v -> onChange { it.copy(start = v) } },
                        label = { Text(stringResource(Res.string.start_time)) }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        editor.end, { v -> onChange { it.copy(end = v) } },
                        label = { Text(stringResource(Res.string.end_time)) }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    editor.breakMinutes, { v -> onChange { it.copy(breakMinutes = v) } },
                    label = { Text(stringResource(Res.string.break_minutes)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                editor.error?.let {
                    Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(Res.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

private fun ShiftEditorError.messageRes() = when (this) {
    ShiftEditorError.NameRequired -> Res.string.error_name_required
    ShiftEditorError.AbbreviationRequired -> Res.string.error_abbreviation_required
    ShiftEditorError.AbbreviationTooLong -> Res.string.error_abbreviation_too_long
    ShiftEditorError.TimesIncomplete -> Res.string.error_times_incomplete
    ShiftEditorError.BreakInvalid -> Res.string.error_break_invalid
    ShiftEditorError.TimeFormat -> Res.string.error_time_format
    ShiftEditorError.BreakFormat -> Res.string.error_break_format
}
