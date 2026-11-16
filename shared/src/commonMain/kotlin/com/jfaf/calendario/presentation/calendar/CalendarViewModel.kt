package com.jfaf.calendario.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.usecase.ObserveShiftsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Sólo depende de casos de uso; expone un único `StateFlow<UiState>`. */
class CalendarViewModel(observeShifts: ObserveShiftsUseCase) : ViewModel() {
    val uiState: StateFlow<CalendarUiState> = observeShifts()
        .map { CalendarUiState(shifts = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())
}
