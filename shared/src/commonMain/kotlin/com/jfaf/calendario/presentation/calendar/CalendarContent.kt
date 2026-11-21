package com.jfaf.calendario.presentation.calendar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jfaf.calendario.domain.model.CalendarDay
import com.jfaf.calendario.presentation.calendar.components.*
import com.jfaf.calendario.presentation.calendar.components.ShiftConflictDialog
import com.jfaf.calendario.presentation.common.color
import com.jfaf.calendario.presentation.social.RequestsChip
import com.jfaf.calendario.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

/** Fracción del ancho de la cuadrícula que hay que arrastrar para cambiar de mes al soltar. */
private const val SWIPE_THRESHOLD = 0.25f
private const val SLIDE_MS = 180

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

    when (val open = state.dialog) {
        CalendarDialog.Assign -> AssignDialog(state.shifts, actions.onDismissDialog) { shiftId, asSecond ->
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            actions.onAssign(shiftId, asSecond)
        }
        CalendarDialog.Repeat -> RepeatDialog(actions.onDismissDialog, actions.onRepeat)
        is CalendarDialog.ShiftConflict -> ShiftConflictDialog(open.dates, actions.onDismissDialog)
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
