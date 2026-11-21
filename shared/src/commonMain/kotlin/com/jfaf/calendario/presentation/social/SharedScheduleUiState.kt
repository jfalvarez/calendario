package com.jfaf.calendario.presentation.social

import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.LinkDirection
import com.jfaf.calendario.domain.model.MyShiftOption
import com.jfaf.calendario.domain.model.SharedAssignment
import com.jfaf.calendario.domain.model.SwapDayPreview
import com.jfaf.calendario.domain.model.SwapProposal
import com.jfaf.calendario.domain.model.SwapStatus
import com.jfaf.calendario.domain.model.previewForMe
import com.jfaf.calendario.domain.repository.SocialError

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
