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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import com.jfaf.calendario.domain.model.AppStyle
import com.jfaf.calendario.presentation.theme.LocalAppStyle
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.jfaf.calendario.domain.model.CalendarDay
import com.jfaf.calendario.domain.model.DayNoteDraft
import com.jfaf.calendario.domain.model.DayNoteMarkers
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.SwapHoriz
import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.resources.*
import com.jfaf.calendario.presentation.common.color
import com.jfaf.calendario.presentation.common.onShiftColor
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import com.jfaf.calendario.presentation.social.RequestsChip
import com.jfaf.calendario.presentation.social.SocialViewModel
import org.koin.compose.viewmodel.koinViewModel

/** Fracción del ancho de la cuadrícula que hay que arrastrar para cambiar de mes al soltar. */
private const val SWIPE_THRESHOLD = 0.25f
private const val SLIDE_MS = 180

/** Acciones que la pantalla envía al ViewModel (agrupadas para no arrastrar quince parámetros). */
class CalendarActions(
    val onPreviousMonth: () -> Unit = {},
    val onNextMonth: () -> Unit = {},
    val onToday: () -> Unit = {},
    val onOpenShifts: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onOpenStats: () -> Unit = {},
    val onOpenDebts: () -> Unit = {},
    val onModeChange: (CalendarMode) -> Unit = {},
    val onShiftSelected: (String) -> Unit = {},
    val onDayClick: (LocalDate) -> Unit = {},
    val onDayLongPress: (LocalDate) -> Unit = {},
    val onClearSelection: () -> Unit = {},
    val onCopy: () -> Unit = {},
    val onCut: () -> Unit = {},
    val onPaste: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onToggleLocalHoliday: () -> Unit = {},
    val onAssignClick: () -> Unit = {},
    val onRepeatClick: () -> Unit = {},
    val onDismissDialog: () -> Unit = {},
    val onAssign: (shiftId: String, asSecond: Boolean) -> Unit = { _, _ -> },
    val onRepeat: (times: Int) -> Unit = {},
    val onSyncClick: () -> Unit = {},
    val onDismissNotes: () -> Unit = {},
    val onSaveNote: (DayNoteDraft) -> Unit = {},
    val onSettleNote: (id: String, settled: Boolean) -> Unit = { _, _ -> },
    val onDeleteNote: (id: String) -> Unit = {},
)

/** Pantalla con estado (conecta el ViewModel) y contenido sin estado (testeable/previsualizable). */
@Composable
fun CalendarScreen(
    onOpenShifts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenDebts: () -> Unit,
    viewModel: CalendarViewModel = koinViewModel(),
    socialViewModel: SocialViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val social by socialViewModel.uiState.collectAsState()
    val actions = remember(viewModel, onOpenShifts, onOpenSettings, onOpenStats, onOpenDebts) {
        CalendarActions(
            onPreviousMonth = viewModel::onPreviousMonth,
            onNextMonth = viewModel::onNextMonth,
            onToday = viewModel::onToday,
            onOpenShifts = onOpenShifts,
            onOpenSettings = onOpenSettings,
            onOpenStats = onOpenStats,
            onOpenDebts = onOpenDebts,
            onModeChange = viewModel::onModeChange,
            onShiftSelected = viewModel::onShiftSelected,
            onDayClick = viewModel::onDayClick,
            onDayLongPress = viewModel::onDayLongPress,
            onClearSelection = viewModel::onClearSelection,
            onCopy = viewModel::onCopy,
            onCut = viewModel::onCut,
            onPaste = viewModel::onPaste,
            onDelete = viewModel::onDelete,
            onToggleLocalHoliday = viewModel::onToggleLocalHoliday,
            onAssignClick = viewModel::onAssignClick,
            onRepeatClick = viewModel::onRepeatClick,
            onDismissDialog = viewModel::onDismissDialog,
            onAssign = viewModel::onAssign,
            onRepeat = viewModel::onRepeat,
            onSyncClick = viewModel::onSyncClick,
            onDismissNotes = viewModel::onDismissNotes,
            onSaveNote = viewModel::onSaveNote,
            onSettleNote = viewModel::onSettleNote,
            onDeleteNote = viewModel::onDeleteNote,
        )
    }
    // En releaseCheck, sin solicitudes reales, se cuenta una de ejemplo para revisar el chip.
    val requests = social.pending.size
    CalendarContent(state, actions, pendingRequests = requests, onOpenRequests = onOpenSettings)
}

@Composable
fun CalendarContent(
    state: CalendarUiState,
    actions: CalendarActions,
    pendingRequests: Int = 0,
    onOpenRequests: () -> Unit = {},
) {
    val monthNames = stringArrayResource(Res.array.month_names)
    val weekdays = stringArrayResource(Res.array.weekday_short)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    // Deslizamiento horizontal de la cuadrícula: `slide` es su desplazamiento en píxeles.
    val slide = remember { Animatable(0f) }
    var gridWidth by remember { mutableFloatStateOf(0f) }
    // +1 = mes siguiente, -1 = mes anterior, 0 = ninguno pendiente de entrar.
    var pendingDirection by remember { mutableIntStateOf(0) }
    // Cuando el ViewModel ya muestra el nuevo mes, la cuadrícula entra desde el lado opuesto.
    LaunchedEffect(state.month) {
        if (pendingDirection != 0) {
            // El mes vecino ya ocupa exactamente esa posición: basta con recolocar la tira sin animar.
            slide.snapTo(0f)
            pendingDirection = 0
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = actions.onPreviousMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(Res.string.previous_month))
            }
            Text(
                "${monthNames[state.month.monthNumber - 1]} ${state.month.year}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            FilledTonalIconButton(onClick = actions.onNextMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(Res.string.next_month))
            }
            FilledTonalButton(onClick = actions.onToday) { Text(stringResource(Res.string.today)) }
            OverflowMenu(actions.onOpenShifts, actions.onOpenStats, actions.onOpenDebts, actions.onOpenSettings)
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
            FilterChip(
                selected = state.mode == CalendarMode.Notes,
                onClick = { actions.onModeChange(CalendarMode.Notes) },
                label = { Text(stringResource(Res.string.mode_notes)) },
            )
            Spacer(Modifier.weight(1f))
            if (pendingRequests > 0) RequestsChip(pendingRequests, onOpenRequests)
            state.syncIndicator?.let { SyncStatus(it, actions.onSyncClick) }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            weekdays.forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { gridWidth = it.width.toFloat() }
                .clipToBounds()
                .pointerInput(actions) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val w = gridWidth
                            val x = slide.value
                            val dir = when {
                                x <= -w * SWIPE_THRESHOLD -> 1
                                x >= w * SWIPE_THRESHOLD -> -1
                                else -> 0
                            }
                            scope.launch {
                                if (dir == 0) {
                                    slide.animateTo(0f, tween(SLIDE_MS))
                                } else {
                                    // Sale la cuadrícula actual y, al terminar, se cambia el mes.
                                    slide.animateTo(-dir * w, tween(SLIDE_MS))
                                    pendingDirection = dir
                                    if (dir > 0) actions.onNextMonth() else actions.onPreviousMonth()
                                }
                            }
                        },
                        onDragCancel = { scope.launch { slide.animateTo(0f, tween(SLIDE_MS)) } },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            if (pendingDirection == 0) {
                                scope.launch { slide.snapTo(slide.value + dragAmount) }
                            }
                        },
                    )
                },
        ) {
            val renderDay: @Composable RowScope.(CalendarDay) -> Unit = { day ->
                DayCell(
                    day = day,
                    isToday = day.date == state.today,
                    isSelected = day.date in state.selection,
                    markers = state.noteMarkers[day.date],
                    onClick = {
                        // Pintar un turno en el día: respuesta háptica (en escritorio no hace nada).
                        if (state.mode == CalendarMode.Paint && state.paintShiftId != null) {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        }
                        actions.onDayClick(day.date)
                    },
                    onLongClick = { actions.onDayLongPress(day.date) },
                )
            }
            MonthWeeks(state.prevWeeks, Modifier.fillMaxSize().graphicsLayer { translationX = slide.value - gridWidth }, renderDay)
            MonthWeeks(state.weeks, Modifier.fillMaxSize().graphicsLayer { translationX = slide.value }, renderDay)
            MonthWeeks(state.nextWeeks, Modifier.fillMaxSize().graphicsLayer { translationX = slide.value + gridWidth }, renderDay)
        }
        Surface(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                when (state.mode) {
                    CalendarMode.Paint -> PaintBar(state, actions)
                    CalendarMode.Edit -> EditBar(state, actions)
                    CalendarMode.Notes -> Text(
                        stringResource(Res.string.notes_mode_hint),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }

    when (state.dialog) {
        CalendarDialog.Assign -> AssignDialog(state.shifts, actions.onDismissDialog) { shiftId, asSecond ->
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            actions.onAssign(shiftId, asSecond)
        }
        CalendarDialog.Repeat -> RepeatDialog(actions.onDismissDialog, actions.onRepeat)
        null -> Unit
    }

    state.notesDialog?.let { notes ->
        NotesDialog(
            state = notes,
            monthName = monthNames[notes.date.month.ordinal],
            onDismiss = actions.onDismissNotes,
            onSave = actions.onSaveNote,
            onSettle = actions.onSettleNote,
            onDelete = actions.onDeleteNote,
        )
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
private fun EditBar(state: CalendarUiState, actions: CalendarActions) {
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
    markers: DayNoteMarkers?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val style = LocalAppStyle.current
    val colors = MaterialTheme.colorScheme
    val first = day.shifts.firstOrNull()
    // Pop: el día entero se pinta con el color de su primer turno.
    val fillShift = if (style == AppStyle.Pop) first else null
    val base = when {
        fillShift != null -> fillShift.color
        style == AppStyle.Sorbete && isToday -> colors.primary
        style == AppStyle.Neon -> Color.Transparent
        else -> colors.surfaceContainerLow
    }
    val background by animateColorAsState(if (isSelected) colors.primaryContainer else base)
    val scale by animateFloatAsState(if (isSelected) 0.93f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    val shape = MaterialTheme.shapes.small
    // Festivo (nacional o local): el número del día siempre en rojo, sea cual sea el fondo.
    val numberColor = when {
        day.holiday != null -> colors.error
        isSelected -> colors.onPrimaryContainer
        fillShift != null -> fillShift.color.onShiftColor
        style == AppStyle.Sorbete && isToday -> colors.onPrimary
        else -> colors.onSurface
    }
    val outline = when {
        isSelected -> colors.primary
        style == AppStyle.Pop && isToday -> colors.onSurface
        else -> null
    }
    Column(
        Modifier
            .weight(1f)
            .fillMaxSize()
            .padding(2.dp)
            .scale(scale)
            .clip(shape)
            .background(background)
            .then(if (outline != null) Modifier.border(if (isSelected) 2.dp else 3.dp, outline, shape) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(3.dp)
            .alpha(if (day.inCurrentMonth) 1f else 0.35f),
        horizontalAlignment = if (style == AppStyle.Neon) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (style == AppStyle.Neon) {
            Box(
                Modifier.size(24.dp).background(if (isToday) colors.primary else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${day.date.day}", fontSize = 12.sp,
                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isToday && day.holiday == null) colors.onPrimary else numberColor,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                day.shifts.forEach { Box(Modifier.size(8.dp).background(it.color, CircleShape)) }
            }
        } else {
            Text(
                "${day.date.day}", fontSize = 12.sp,
                fontWeight = if (isToday || fillShift != null) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = numberColor,
            )
            if (fillShift != null) {
                Text(fillShift.abbreviation, color = fillShift.color.onShiftColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                day.shifts.drop(1).forEach { ShiftTag(it, Modifier.fillMaxWidth()) }
            } else {
                day.shifts.forEach { ShiftTag(it, Modifier.fillMaxWidth()) }
            }
        }
if (markers != null) NoteMarker(markers)
    }
}

/** Cuadrícula de un mes: una fila por semana, cada día lo dibuja [dayCell]. */
@Composable
private fun MonthWeeks(
    weeks: List<List<CalendarDay>>,
    modifier: Modifier,
    dayCell: @Composable RowScope.(CalendarDay) -> Unit,
) {
    Column(modifier) {
        weeks.forEach { week ->
            Row(Modifier.weight(1f).fillMaxWidth()) {
                week.forEach { day -> dayCell(day) }
            }
        }
    }
}

@Composable
private fun ShiftTag(shift: Shift, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = 17.dp)
            .background(shift.color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(shift.abbreviation, color = shift.color.onShiftColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun OverflowMenu(onOpenShifts: () -> Unit, onOpenStats: () -> Unit, onOpenDebts: () -> Unit, onOpenSettings: () -> Unit) {
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

/** Icono y texto breve del estado de sincronización; al tocarlo se sincroniza. */
@Composable
private fun SyncStatus(indicator: SyncIndicator, onClick: () -> Unit) {
    val label = stringResource(
        when (indicator) {
            SyncIndicator.Pending -> Res.string.sync_status_pending
            SyncIndicator.Syncing -> Res.string.sync_status_syncing
            SyncIndicator.Synced -> Res.string.sync_status_synced
            SyncIndicator.Offline -> Res.string.sync_status_offline
            SyncIndicator.Error -> Res.string.sync_status_error
        }
    )
    val problem = indicator == SyncIndicator.Offline || indicator == SyncIndicator.Error
    val color = if (problem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = indicator != SyncIndicator.Syncing, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (indicator == SyncIndicator.Syncing) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = color)
        } else {
            Icon(
                when (indicator) {
                    SyncIndicator.Synced -> Icons.Filled.CloudDone
                    SyncIndicator.Offline -> Icons.Filled.CloudOff
                    SyncIndicator.Error -> Icons.Filled.SyncProblem
                    else -> Icons.Filled.CloudQueue
                },
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}

/**
 * Píldora de ancho completo, como la de un turno, para el día con notas y/o turnos debidos sin saldar. Lleva colores
 * propios (no los de un turno) para no confundirse con ellos: terciario si hay deuda pendiente, secundario si sólo hay nota.
 */
@Composable
private fun NoteMarker(markers: DayNoteMarkers) {
    val colors = MaterialTheme.colorScheme
    val container = if (markers.hasOpenDebt) colors.tertiaryContainer else colors.secondaryContainer
    val content = if (markers.hasOpenDebt) colors.onTertiaryContainer else colors.onSecondaryContainer
    Row(
        Modifier.fillMaxWidth().heightIn(min = 17.dp).background(container, CircleShape),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (markers.hasNote) Icon(Icons.Filled.StickyNote2, null, Modifier.size(13.dp), tint = content)
        if (markers.hasOpenDebt) Icon(Icons.Filled.SwapHoriz, null, Modifier.size(15.dp), tint = content)
    }
}
