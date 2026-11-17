package com.jfaf.calendario.presentation.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.model.PersonBalance
import com.jfaf.calendario.domain.usecase.ObserveDebtsUseCase
import com.jfaf.calendario.domain.usecase.SetNoteSettledUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DebtsViewModel(
    observeDebts: ObserveDebtsUseCase,
    private val setSettled: SetNoteSettledUseCase,
) : ViewModel() {
    /** `null` mientras se leen los datos (evita parpadear «no hay deudas»). */
    val balances: StateFlow<List<PersonBalance>?> =
        observeDebts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onSettle(id: String, settled: Boolean) {
        viewModelScope.launch { setSettled(id, settled) }
    }
}
