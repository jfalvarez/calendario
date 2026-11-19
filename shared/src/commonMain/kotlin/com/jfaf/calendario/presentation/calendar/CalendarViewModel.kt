package com.jfaf.calendario.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.repository.TodayProvider
import com.jfaf.calendario.domain.usecase.ObserveMonthUseCase
import com.jfaf.calendario.domain.usecase.ObserveNoteMarkersUseCase
import com.jfaf.calendario.domain.usecase.ObserveShiftsUseCase
import com.jfaf.calendario.domain.usecase.PaintDayUseCase
import com.jfaf.calendario.presentation.update.AppForegroundEvents
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

/**
 * Mes mostrado, modo y pintar. Las selecciones, las notas y la sincronización tienen sus propios ViewModels;
 * la pantalla compone los estados.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    observeMonth: ObserveMonthUseCase,
    observeShifts: ObserveShiftsUseCase,
    observeNoteMarkers: ObserveNoteMarkersUseCase,
    private val paintDay: PaintDayUseCase,
    private val todayProvider: TodayProvider,
    foregroundEvents: AppForegroundEvents,
) : ViewModel() {
    private var today = todayProvider.today()
    private val month = MutableStateFlow(CalendarMonth.of(today))
    private val paintShiftId = MutableStateFlow<String?>(null)
    private val mode = MutableStateFlow(CalendarMode.Paint)

    init {
        // Al volver a primer plano: recalcula "hoy" (puede haber cambiado de día) y sitúa el calendario en el mes actual.
        viewModelScope.launch {
            foregroundEvents.events.collect {
                today = todayProvider.today()
                month.value = CalendarMonth.of(today)
            }
        }
    }

    /** Cuadrículas del mes mostrado y de sus vecinos: al deslizar, el vecino ya llega con sus turnos. */
    private val threeMonths = month.flatMapLatest { m ->
        combine(observeMonth(m.previous), observeMonth(m), observeMonth(m.next)) { prev, cur, next ->
            Triple(prev, cur, next)
        }
    }

    private val markers = month.flatMapLatest { observeNoteMarkers(it) }

    val uiState: StateFlow<CalendarUiState> = combine(
        combine(month, threeMonths, observeShifts(), paintShiftId, mode) { m, weeks, shifts, paintId, md ->
            CalendarUiState(
                month = m, today = today, weeks = weeks.second, prevWeeks = weeks.first, nextWeeks = weeks.third,
                shifts = shifts, paintShiftId = paintId, mode = md,
            )
        },
        markers,
    ) { base, noteMarkers -> base.copy(noteMarkers = noteMarkers) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState(month = month.value, today = today))

    fun onPreviousMonth() = month.update { it.previous }
    fun onNextMonth() = month.update { it.next }
    fun onToday() { month.value = CalendarMonth.of(today) }

    fun onModeChange(newMode: CalendarMode) { mode.value = newMode }

    /** Pulsar el turno activo lo desactiva. */
    fun onShiftSelected(id: String) = paintShiftId.update { if (it == id) null else id }

    /** Modo Pintar: pone el turno activo en el día tocado (si hay uno activo). */
    fun onPaintDay(date: LocalDate) {
        val id = paintShiftId.value ?: return
        viewModelScope.launch { paintDay(date, id) }
    }
}
