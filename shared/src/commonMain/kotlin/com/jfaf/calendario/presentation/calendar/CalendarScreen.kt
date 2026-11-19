package com.jfaf.calendario.presentation.calendar

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jfaf.calendario.domain.model.DayNoteDraft
import com.jfaf.calendario.presentation.calendar.components.*
import com.jfaf.calendario.presentation.social.SocialViewModel
import com.jfaf.calendario.resources.*
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

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

/** Pantalla con estado: compone los estados de los ViewModels y reenvía cada acción al que la gestiona. */
@Composable
fun CalendarScreen(
    onOpenShifts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenDebts: () -> Unit,
    viewModel: CalendarViewModel = koinViewModel(),
    editingViewModel: DayEditingViewModel = koinViewModel(),
    notesViewModel: DayNotesViewModel = koinViewModel(),
    syncViewModel: SyncStatusViewModel = koinViewModel(),
    socialViewModel: SocialViewModel = koinViewModel(),
) {
    val base by viewModel.uiState.collectAsState()
    val edit by editingViewModel.uiState.collectAsState()
    val notes by notesViewModel.uiState.collectAsState()
    val sync by syncViewModel.indicator.collectAsState()
    val social by socialViewModel.uiState.collectAsState()
    val state = base.copy(
        selection = edit.selection,
        hasClipboard = edit.hasClipboard,
        dialog = edit.dialog,
        syncIndicator = sync,
        notesDialog = notes,
    )
    // Las acciones leen `base` al pulsarse, así que siempre ven el modo actual.
    val actions = remember(viewModel, editingViewModel, notesViewModel, syncViewModel, onOpenShifts, onOpenSettings, onOpenStats, onOpenDebts) {
        CalendarActions(
            onPreviousMonth = viewModel::onPreviousMonth,
            onNextMonth = viewModel::onNextMonth,
            onToday = viewModel::onToday,
            onOpenShifts = onOpenShifts,
            onOpenSettings = onOpenSettings,
            onOpenStats = onOpenStats,
            onOpenDebts = onOpenDebts,
            onModeChange = { newMode ->
                viewModel.onModeChange(newMode)
                editingViewModel.onReset()
                notesViewModel.onReset()
            },
            onShiftSelected = viewModel::onShiftSelected,
            onDayClick = { date ->
                when (base.mode) {
                    CalendarMode.Paint -> viewModel.onPaintDay(date)
                    CalendarMode.Edit -> editingViewModel.onDayToggle(date)
                    CalendarMode.Notes -> notesViewModel.onDayClick(date)
                }
            },
            onDayLongPress = { date -> if (base.mode == CalendarMode.Edit) editingViewModel.onDayLongPress(date) },
            onClearSelection = editingViewModel::onClearSelection,
            onCopy = editingViewModel::onCopy,
            onCut = editingViewModel::onCut,
            onPaste = editingViewModel::onPaste,
            onDelete = editingViewModel::onDelete,
            onToggleLocalHoliday = editingViewModel::onToggleLocalHoliday,
            onAssignClick = editingViewModel::onAssignClick,
            onRepeatClick = editingViewModel::onRepeatClick,
            onDismissDialog = editingViewModel::onDismissDialog,
            onAssign = editingViewModel::onAssign,
            onRepeat = editingViewModel::onRepeat,
            onSyncClick = syncViewModel::onSyncClick,
            onDismissNotes = notesViewModel::onDismissNotes,
            onSaveNote = notesViewModel::onSaveNote,
            onSettleNote = notesViewModel::onSettleNote,
            onDeleteNote = notesViewModel::onDeleteNote,
        )
    }
    // En releaseCheck, sin solicitudes reales, se cuenta una de ejemplo para revisar el chip.
    val requests = social.pending.size
    CalendarContent(state, actions, pendingRequests = requests, onOpenRequests = onOpenSettings)
}
