package com.jfaf.calendario.presentation.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.jfaf.calendario.domain.model.CalendarDay
import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.resources.*
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Acciones que la pantalla envía al ViewModel (agrupadas para no arrastrar quince parámetros). */
class CalendarActions(
    val onPreviousMonth: () -> Unit = {},
    val onNextMonth: () -> Unit = {},
    val onToday: () -> Unit = {},
    val onOpenShifts: () -> Unit = {},
    val onModeChange: (CalendarMode) -> Unit = {},
    val onShiftSelected: (String) -> Unit = {},
    val onDayClick: (LocalDate) -> Unit = {},
    val onDayLongPress: (LocalDate) -> Unit = {},
    val onClearSelection: () -> Unit = {},
    val onCopy: () -> Unit = {},
    val onCut: () -> Unit = {},
    val onPaste: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onAssignClick: () -> Unit = {},
    val onRepeatClick: () -> Unit = {},
    val onDismissDialog: () -> Unit = {},
    val onAssign: (shiftId: String, asSecond: Boolean) -> Unit = { _, _ -> },
    val onRepeat: (times: Int) -> Unit = {},
)

/** Pantalla con estado (conecta el ViewModel) y contenido sin estado (testeable/previsualizable). */
@Composable
fun CalendarScreen(onOpenShifts: () -> Unit, viewModel: CalendarViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val actions = remember(viewModel, onOpenShifts) {
        CalendarActions(
            onPreviousMonth = viewModel::onPreviousMonth,
            onNextMonth = viewModel::onNextMonth,
            onToday = viewModel::onToday,
            onOpenShifts = onOpenShifts,
            onModeChange = viewModel::onModeChange,
            onShiftSelected = viewModel::onShiftSelected,
            onDayClick = viewModel::onDayClick,
            onDayLongPress = viewModel::onDayLongPress,
            onClearSelection = viewModel::onClearSelection,
            onCopy = viewModel::onCopy,
            onCut = viewModel::onCut,
            onPaste = viewModel::onPaste,
            onDelete = viewModel::onDelete,
            onAssignClick = viewModel::onAssignClick,
            onRepeatClick = viewModel::onRepeatClick,
            onDismissDialog = viewModel::onDismissDialog,
            onAssign = viewModel::onAssign,
            onRepeat = viewModel::onRepeat,
        )
    }
    CalendarContent(state, actions)
}

@Composable
fun CalendarContent(state: CalendarUiState, actions: CalendarActions) {
    val monthNames = stringArrayResource(Res.array.month_names)
    val weekdays = stringArrayResource(Res.array.weekday_short)
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = actions.onPreviousMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(Res.string.previous_month))
            }
            Text(
                "${monthNames[state.month.monthNumber - 1]} ${state.month.year}",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = actions.onNextMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(Res.string.next_month))
            }
            TextButton(onClick = actions.onToday) { Text(stringResource(Res.string.today)) }
            IconButton(onClick = actions.onOpenShifts) {
                Icon(Icons.AutoMirrored.Filled.List, stringResource(Res.string.manage_shifts))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.mode == CalendarMode.Paint,
                onClick = { actions.onModeChange(CalendarMode.Paint) },
                label = { Text(stringResource(Res.string.mode_paint)) },
            )
            FilterChip(
                selected = state.mode == CalendarMode.Edit,
                onClick = { actions.onModeChange(CalendarMode.Edit) },
                label = { Text(stringResource(Res.string.mode_edit)) },
            )
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            weekdays.forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth()) {
            state.weeks.forEach { week ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    week.forEach { day ->
                        DayCell(
                            day = day,
                            isToday = day.date == state.today,
                            isSelected = day.date in state.selection,
                            onClick = { actions.onDayClick(day.date) },
                            onLongClick = { actions.onDayLongPress(day.date) },
                        )
                    }
                }
            }
        }
        when (state.mode) {
            CalendarMode.Paint -> PaintBar(state, actions)
            CalendarMode.Edit -> EditBar(state, actions)
        }
    }

    when (state.dialog) {
        CalendarDialog.Assign -> AssignDialog(state.shifts, actions.onDismissDialog, actions.onAssign)
        CalendarDialog.Repeat -> RepeatDialog(actions.onDismissDialog, actions.onRepeat)
        null -> Unit
    }
}

@Composable
private fun PaintBar(state: CalendarUiState, actions: CalendarActions) {
    Text(
        stringResource(Res.string.paint_mode_hint),
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 8.dp),
    )
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.shifts.forEach { shift ->
            FilterChip(
                selected = state.paintShiftId == shift.id,
                onClick = { actions.onShiftSelected(shift.id) },
                label = { Text("${shift.abbreviation} · ${shift.name}") },
            )
        }
    }
}

@Composable
private fun EditBar(state: CalendarUiState, actions: CalendarActions) {
    val hasSelection = state.selection.isNotEmpty()
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
        ActionButton(Icons.Filled.ContentPaste, Res.string.paste, hasSelection && state.hasClipboard, actions.onPaste)
        ActionButton(Icons.Filled.Repeat, Res.string.repeat, hasSelection, actions.onRepeatClick)
        ActionButton(Icons.Filled.Delete, Res.string.delete, hasSelection, actions.onDelete)
        ActionButton(Icons.Filled.Close, Res.string.clear_selection, hasSelection, actions.onClearSelection)
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: org.jetbrains.compose.resources.StringResource,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled) { Icon(icon, stringResource(label)) }
}

@Composable
private fun AssignDialog(
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

@Composable
private fun RepeatDialog(onDismiss: () -> Unit, onRepeat: (Int) -> Unit) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.DayCell(
    day: CalendarDay,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val border = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val background = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    Column(
        Modifier
            .weight(1f)
            .fillMaxSize()
            .padding(1.dp)
            .background(background, RoundedCornerShape(4.dp))
            .border(if (isToday || isSelected) 2.dp else 1.dp, border, RoundedCornerShape(4.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(2.dp)
            .alpha(if (day.inCurrentMonth) 1f else 0.4f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text("${day.date.day}", fontSize = 12.sp)
        day.shifts.forEach { ShiftTag(it, Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun ShiftTag(shift: Shift, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = 16.dp)
            .background(Color(shift.colorArgb), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(shift.abbreviation, color = Color.White, fontSize = 11.sp, maxLines = 1)
    }
}
