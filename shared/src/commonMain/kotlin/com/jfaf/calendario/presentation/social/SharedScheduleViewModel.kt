package com.jfaf.calendario.presentation.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.LinkDirection
import com.jfaf.calendario.domain.model.SharedAssignment
import com.jfaf.calendario.domain.model.SwapDayPreview
import com.jfaf.calendario.domain.model.SwapProposal
import com.jfaf.calendario.domain.model.SwapStatus
import com.jfaf.calendario.domain.model.previewForMe
import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import com.jfaf.calendario.domain.repository.ShiftRepository
import com.jfaf.calendario.domain.repository.SocialError
import com.jfaf.calendario.domain.repository.SocialException
import com.jfaf.calendario.domain.repository.SocialRepository
import com.jfaf.calendario.domain.repository.TodayProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate

/** Un turno mío que puedo entregar en un cambio. `slot` es 0 o 1. */
data class MyShiftOption(val date: LocalDate, val slot: Int, val shiftName: String)

/** Propuesta en curso sobre un turno del compañero: el turno de él y los míos que puedo ofrecer. */
data class ProposalDraft(
    val target: SharedAssignment,
    val options: List<MyShiftOption>,
    val selected: MyShiftOption? = null,
    val sending: Boolean = false,
    val error: SocialError? = null,
) {
    /** Vista previa antes de enviar: lo que entrego pasa a libre y lo que recibo aparece en mi calendario. */
    val preview: List<SwapDayPreview>
        get() = selected?.let { mine ->
            SwapProposal(
                id = "",
                direction = LinkDirection.Outgoing,
                otherAlias = null,
                proposerDate = mine.date,
                proposerSlot = mine.slot,
                proposerShift = mine.shiftName,
                targetDate = target.date,
                targetSlot = target.slot,
                targetShift = target.shiftName,
                status = SwapStatus.Pending,
            ).previewForMe()
        }.orEmpty()
}

/** Calendario de solo lectura de un compañero: mes mostrado, sus turnos, y la propuesta de cambio en curso. */
data class SharedScheduleUiState(
    val alias: String,
    val month: CalendarMonth,
    val assignments: List<SharedAssignment> = emptyList(),
    val loading: Boolean = true,
    val error: SocialError? = null,
    val proposal: ProposalDraft? = null,
)

class SharedScheduleViewModel(
    private val ownerId: String,
    private val alias: String,
    private val social: SocialRepository,
    today: TodayProvider,
    private val localAssignments: ShiftAssignmentRepository,
    private val localShifts: ShiftRepository,
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
            val result = social.sharedSchedule(ownerId, month)
            // Si mientras cargaba el usuario cambió de mes, se descarta esta respuesta.
            if (state.value.month != month) return@launch
            state.update {
                it.copy(
                    assignments = result.getOrDefault(emptyList()),
                    loading = false,
                    error = result.exceptionOrNull()?.let { e -> (e as? SocialException)?.error ?: SocialError.Unknown },
                )
            }
        }
    }

    /** Abre la propuesta sobre un turno del compañero, con mis turnos de los próximos 60 días como opciones. */
    fun openProposal(target: SharedAssignment) {
        viewModelScope.launch {
            val names = localShifts.observeShifts().first().associate { it.id to it.name }
            val from = todayDate
            val to = LocalDate.fromEpochDays(from.toEpochDays() + 60)
            val options = localAssignments.getAssignments(from, to)
                .flatMap { (date, ids) -> ids.mapIndexedNotNull { slot, id ->
                    names[id]?.let { MyShiftOption(date, slot, it) }
                } }
                .sortedWith(compareBy({ it.date }, { it.slot }))
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
            val result = social.proposeSwap(ownerId, mine.date, mine.slot, draft.target.date, draft.target.slot)
            state.update {
                it.copy(
                    proposal = if (result.isSuccess) null else draft.copy(
                        sending = false,
                        error = (result.exceptionOrNull() as? SocialException)?.error ?: SocialError.Unknown,
                    ),
                )
            }
        }
    }
}
