package com.jfaf.calendario.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.model.DayPattern
import com.jfaf.calendario.domain.usecase.AssignShiftToDaysUseCase
import com.jfaf.calendario.domain.usecase.ClearDaysUseCase
import com.jfaf.calendario.domain.usecase.CopyDaysUseCase
import com.jfaf.calendario.domain.usecase.PasteDaysUseCase
import com.jfaf.calendario.domain.usecase.RepeatDaysUseCase
import com.jfaf.calendario.domain.usecase.ToggleLocalHolidaysUseCase
import com.jfaf.calendario.domain.usecase.dateRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** Estado del modo Editar: días seleccionados, si hay algo copiado y el diálogo abierto. */
data class DayEditingState(
    val selection: Set<LocalDate> = emptySet(),
    val hasClipboard: Boolean = false,
    val dialog: CalendarDialog? = null,
)

/** Selección de días y acciones sobre ellos (copiar, cortar, pegar, borrar, festivos, asignar, repetir). */
class DayEditingViewModel(
    private val copyDays: CopyDaysUseCase,
    private val pasteDays: PasteDaysUseCase,
    private val clearDays: ClearDaysUseCase,
    private val assignShift: AssignShiftToDaysUseCase,
    private val repeatDays: RepeatDaysUseCase,
    private val toggleLocalHolidays: ToggleLocalHolidaysUseCase,
) : ViewModel() {
    private val selection = MutableStateFlow<Set<LocalDate>>(emptySet())
    private val clipboard = MutableStateFlow<DayPattern?>(null)
    private val dialog = MutableStateFlow<CalendarDialog?>(null)

    /** Último día tocado: extremo desde el que se selecciona un rango con pulsación larga. */
    private var anchor: LocalDate? = null

    val uiState: StateFlow<DayEditingState> = combine(selection, clipboard, dialog) { s, c, d ->
        DayEditingState(selection = s, hasClipboard = c != null, dialog = d)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayEditingState())

    /** Tocar un día añade o quita su selección. */
    fun onDayToggle(date: LocalDate) {
        selection.update { if (date in it) it - date else it + date }
        anchor = date
    }

    /** Pulsación larga: selecciona todos los días entre el último tocado y éste. */
    fun onDayLongPress(date: LocalDate) {
        val from = anchor ?: date
        selection.update { it + dateRange(from, date) }
        anchor = date
    }

    fun onClearSelection() {
        selection.value = emptySet()
        anchor = null
    }

    /** Al cambiar de modo: se olvida la selección y el diálogo, pero no lo copiado. */
    fun onReset() {
        onClearSelection()
        dialog.value = null
    }

    fun onCopy() = withSelection { dates -> clipboard.value = copyDays(dates) }

    fun onCut() = withSelection { dates ->
        clipboard.value = copyDays(dates)
        clearDays(dates)
    }

    /** Pega el bloque copiado empezando en el primer día seleccionado. */
    fun onPaste() {
        val pattern = clipboard.value ?: return
        withSelection { dates -> pasteDays(pattern, dates.min()) }
    }

    fun onDelete() = withSelection { dates -> clearDays(dates) }

    /** Marca los días seleccionados como festivo local (o los desmarca si todos ya lo eran). */
    fun onToggleLocalHoliday() = withSelection { dates -> toggleLocalHolidays(dates) }

    fun onAssignClick() = withSelection { dialog.value = CalendarDialog.Assign }
    fun onRepeatClick() = withSelection { dialog.value = CalendarDialog.Repeat }
    fun onDismissDialog() { dialog.value = null }

    fun onAssign(shiftId: String, asSecond: Boolean) = withSelection { dates ->
        dialog.value = null
        assignShift(dates, shiftId, asSecond)
    }

    fun onRepeat(times: Int) = withSelection { dates ->
        dialog.value = null
        repeatDays(dates, times)
    }

    private fun withSelection(block: suspend (Set<LocalDate>) -> Unit) {
        val dates = selection.value
        if (dates.isEmpty()) return
        viewModelScope.launch { block(dates) }
    }
}
