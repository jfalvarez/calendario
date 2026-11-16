package com.jfaf.calendario.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.DayPattern
import com.jfaf.calendario.domain.repository.TodayProvider
import com.jfaf.calendario.domain.usecase.AssignShiftToDaysUseCase
import com.jfaf.calendario.domain.usecase.ClearDaysUseCase
import com.jfaf.calendario.domain.usecase.CopyDaysUseCase
import com.jfaf.calendario.domain.usecase.ObserveMonthUseCase
import com.jfaf.calendario.domain.usecase.ObserveShiftsUseCase
import com.jfaf.calendario.domain.usecase.PaintDayUseCase
import com.jfaf.calendario.domain.usecase.PasteDaysUseCase
import com.jfaf.calendario.domain.usecase.RepeatDaysUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** Sólo depende de casos de uso; expone un único `StateFlow<UiState>`. */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    observeMonth: ObserveMonthUseCase,
    observeShifts: ObserveShiftsUseCase,
    private val paintDay: PaintDayUseCase,
    private val copyDays: CopyDaysUseCase,
    private val pasteDays: PasteDaysUseCase,
    private val clearDays: ClearDaysUseCase,
    private val assignShift: AssignShiftToDaysUseCase,
    private val repeatDays: RepeatDaysUseCase,
    todayProvider: TodayProvider,
) : ViewModel() {
    private val today = todayProvider.today()
    private val month = MutableStateFlow(CalendarMonth.of(today))
    private val paintShiftId = MutableStateFlow<String?>(null)
    private val mode = MutableStateFlow(CalendarMode.Paint)
    private val selection = MutableStateFlow<Set<LocalDate>>(emptySet())
    private val clipboard = MutableStateFlow<DayPattern?>(null)
    private val dialog = MutableStateFlow<CalendarDialog?>(null)
    /** Último día tocado: extremo desde el que se selecciona un rango con pulsación larga. */
    private var anchor: LocalDate? = null

    private data class EditState(
        val mode: CalendarMode,
        val selection: Set<LocalDate>,
        val hasClipboard: Boolean,
        val dialog: CalendarDialog?,
    )

    private val editState = combine(mode, selection, clipboard, dialog) { m, s, c, d ->
        EditState(m, s, c != null, d)
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        month,
        month.flatMapLatest { observeMonth(it) },
        observeShifts(),
        paintShiftId,
        editState,
    ) { m, weeks, shifts, paintId, edit ->
        CalendarUiState(
            month = m, today = today, weeks = weeks, shifts = shifts, paintShiftId = paintId,
            mode = edit.mode, selection = edit.selection, hasClipboard = edit.hasClipboard, dialog = edit.dialog,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CalendarUiState(month = month.value, today = today),
    )

    fun onPreviousMonth() = month.update { it.previous }
    fun onNextMonth() = month.update { it.next }
    fun onToday() { month.value = CalendarMonth.of(today) }

    fun onModeChange(newMode: CalendarMode) {
        mode.value = newMode
        selection.value = emptySet()
        anchor = null
        dialog.value = null
    }

    /** Pulsar el turno activo lo desactiva. */
    fun onShiftSelected(id: String) = paintShiftId.update { if (it == id) null else id }

    fun onDayClick(date: LocalDate) {
        when (mode.value) {
            CalendarMode.Paint -> paintShiftId.value?.let { id -> viewModelScope.launch { paintDay(date, id) } }
            CalendarMode.Edit -> {
                selection.update { if (date in it) it - date else it + date }
                anchor = date
            }
        }
    }

    /** Pulsación larga (modo Editar): selecciona todos los días entre el último tocado y éste. */
    fun onDayLongPress(date: LocalDate) {
        if (mode.value != CalendarMode.Edit) return
        val from = anchor ?: date
        val (start, end) = if (from <= date) from to date else date to from
        val range = generateSequence(start) { it.plusOneDay() }.takeWhile { it <= end }.toSet()
        selection.update { it + range }
        anchor = date
    }

    fun onClearSelection() {
        selection.value = emptySet()
        anchor = null
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

private fun LocalDate.plusOneDay(): LocalDate =
    kotlinx.datetime.LocalDate.fromEpochDays(toEpochDays() + 1)
