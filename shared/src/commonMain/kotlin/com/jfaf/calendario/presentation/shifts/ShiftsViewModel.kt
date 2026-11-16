package com.jfaf.calendario.presentation.shifts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.usecase.DeleteShiftUseCase
import com.jfaf.calendario.domain.usecase.ObserveShiftsUseCase
import com.jfaf.calendario.domain.usecase.ReorderShiftsUseCase
import com.jfaf.calendario.domain.usecase.SaveShiftUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShiftsViewModel(
    observeShifts: ObserveShiftsUseCase,
    private val saveShift: SaveShiftUseCase,
    private val deleteShift: DeleteShiftUseCase,
    private val reorderShifts: ReorderShiftsUseCase,
) : ViewModel() {
    private val editor = MutableStateFlow<ShiftEditorState?>(null)
    private val pendingDelete = MutableStateFlow<Shift?>(null)
    private var current: List<Shift> = emptyList()

    val uiState: StateFlow<ShiftsUiState> = combine(observeShifts(), editor, pendingDelete) { shifts, ed, del ->
        current = shifts
        ShiftsUiState(shifts, ed, del)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShiftsUiState())

    fun onAdd() { editor.value = ShiftEditorState() }

    fun onEdit(shift: Shift) {
        editor.value = ShiftEditorState(
            id = shift.id,
            name = shift.name,
            abbreviation = shift.abbreviation,
            colorArgb = shift.colorArgb,
            start = shift.startMinutes?.let(::formatMinutes).orEmpty(),
            end = shift.endMinutes?.let(::formatMinutes).orEmpty(),
            breakMinutes = shift.breakMinutes.takeIf { it > 0 }?.toString().orEmpty(),
        )
    }

    fun onEditorChange(change: (ShiftEditorState) -> ShiftEditorState) =
        editor.update { it?.let(change)?.copy(error = null) }

    fun onDismissEditor() { editor.value = null }

    fun onSave() {
        val form = editor.value ?: return
        val start = parseMinutes(form.start)
        val end = parseMinutes(form.end)
        val breakMin = form.breakMinutes.trim().ifEmpty { "0" }.toIntOrNull()
        val formatError = when {
            (form.start.isNotBlank() && start == null) || (form.end.isNotBlank() && end == null) -> ShiftEditorError.TimeFormat
            breakMin == null -> ShiftEditorError.BreakFormat
            else -> null
        }
        if (formatError != null) {
            editor.update { it?.copy(error = formatError) }
            return
        }
        viewModelScope.launch {
            val shift = Shift(form.id, form.name, form.abbreviation, form.colorArgb, start, end, breakMin ?: 0)
            val error = saveShift(shift)
            if (error == null) editor.value = null else editor.update { it?.copy(error = error.toEditorError()) }
        }
    }

    fun onRequestDelete(shift: Shift) { pendingDelete.value = shift }
    fun onCancelDelete() { pendingDelete.value = null }

    fun onConfirmDelete() {
        val shift = pendingDelete.value ?: return
        pendingDelete.value = null
        viewModelScope.launch { deleteShift(shift.id) }
    }

    fun onMove(id: String, delta: Int) {
        val ids = current.map { it.id }.toMutableList()
        val from = ids.indexOf(id)
        val to = from + delta
        if (from < 0 || to !in ids.indices) return
        ids.add(to, ids.removeAt(from))
        viewModelScope.launch { reorderShifts(ids) }
    }
}

/** `HH:mm` o `H:mm` → minutos desde medianoche; null si no es válido (o está vacío). */
internal fun parseMinutes(text: String): Int? {
    val parts = text.trim().split(':')
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    return if (h in 0..23 && m in 0..59 && parts[1].length == 2) h * 60 + m else null
}

internal fun formatMinutes(minutes: Int): String =
    "${(minutes / 60).toString().padStart(2, '0')}:${(minutes % 60).toString().padStart(2, '0')}"

