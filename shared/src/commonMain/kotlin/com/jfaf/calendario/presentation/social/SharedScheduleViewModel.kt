package com.jfaf.calendario.presentation.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.SharedAssignment
import com.jfaf.calendario.domain.repository.SocialError
import com.jfaf.calendario.domain.repository.TodayProvider
import com.jfaf.calendario.domain.repository.toSocialError
import com.jfaf.calendario.domain.model.MyShiftOption
import com.jfaf.calendario.domain.usecase.BuildSwapOptionsUseCase
import com.jfaf.calendario.domain.usecase.LoadSharedScheduleUseCase
import com.jfaf.calendario.domain.usecase.ProposeSwapUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SharedScheduleViewModel(
    private val ownerId: String,
    private val alias: String,
    private val loadSchedule: LoadSharedScheduleUseCase,
    private val proposeSwap: ProposeSwapUseCase,
    private val buildSwapOptions: BuildSwapOptionsUseCase,
    today: TodayProvider,
) : ViewModel() {

    private val state = MutableStateFlow(SharedScheduleUiState(alias, CalendarMonth.of(today.today())))
    val uiState: StateFlow<SharedScheduleUiState> = state.asStateFlow()

    private val todayDate = today.today()

    init {
        load()
    }

    fun onPreviousMonth() = moveTo(state.value.month.previous)
    fun onNextMonth() = moveTo(state.value.month.next)

    /** Recarga el mes actual. */
    fun retry() = load()

    private fun moveTo(month: CalendarMonth) {
        state.update { it.copy(month = month) }
        load()
    }

    private fun load() {
        val month = state.value.month
        state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = loadSchedule(ownerId, month)
            // Si mientras cargaba el usuario cambió de mes, se descarta esta respuesta.
            if (state.value.month != month) return@launch
            state.update {
                it.copy(
                    assignments = result.getOrDefault(emptyList()),
                    loading = false,
                    error = result.exceptionOrNull()?.toSocialError(),
                )
            }
        }
    }

    /** Abre la propuesta sobre un turno del compañero, con mis turnos de los próximos días como opciones. */
    fun openProposal(target: SharedAssignment) {
        viewModelScope.launch {
            val options: List<MyShiftOption> = buildSwapOptions(todayDate)
            state.update { it.copy(proposal = ProposalDraft(target, options)) }
        }
    }

    fun selectOption(option: MyShiftOption) = state.update { s ->
        s.copy(proposal = s.proposal?.copy(selected = option, error = null))
    }

    fun closeProposal() = state.update { it.copy(proposal = null) }

    /** Envía la propuesta. Al cerrarse con éxito, el servidor la guarda como pendiente: aún no cambia nada. */
    fun sendProposal() {
        val draft = state.value.proposal ?: return
        val mine = draft.selected ?: return
        if (draft.sending) return
        state.update { it.copy(proposal = draft.copy(sending = true, error = null)) }
        viewModelScope.launch {
            val result = proposeSwap(ownerId, mine.date, mine.slot, draft.target.date, draft.target.slot)
            state.update {
                it.copy(
                    proposal = if (result.isSuccess) null else draft.copy(
                        sending = false,
                        error = result.exceptionOrNull()?.toSocialError() ?: SocialError.Unknown,
                    ),
                )
            }
        }
    }
}
