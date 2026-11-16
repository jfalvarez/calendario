package com.jfaf.calendario.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.repository.TodayProvider
import com.jfaf.calendario.domain.usecase.ObserveMonthUseCase
import com.jfaf.calendario.domain.usecase.ObserveShiftsUseCase
import com.jfaf.calendario.domain.usecase.PaintDayUseCase
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
    todayProvider: TodayProvider,
) : ViewModel() {
    private val today = todayProvider.today()
    private val month = MutableStateFlow(CalendarMonth.of(today))
    private val paintShiftId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<CalendarUiState> = combine(
        month,
        month.flatMapLatest { observeMonth(it) },
        observeShifts(),
        paintShiftId,
    ) { m, weeks, shifts, paintId ->
        CalendarUiState(month = m, today = today, weeks = weeks, shifts = shifts, paintShiftId = paintId)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CalendarUiState(month = month.value, today = today),
    )

    fun onPreviousMonth() = month.update { it.previous }
    fun onNextMonth() = month.update { it.next }
    fun onToday() { month.value = CalendarMonth.of(today) }

    /** Pulsar el turno activo lo desactiva. */
    fun onShiftSelected(id: Long) = paintShiftId.update { if (it == id) null else id }

    fun onDayClick(date: LocalDate) {
        val id = paintShiftId.value ?: return
        viewModelScope.launch { paintDay(date, id) }
    }
}
