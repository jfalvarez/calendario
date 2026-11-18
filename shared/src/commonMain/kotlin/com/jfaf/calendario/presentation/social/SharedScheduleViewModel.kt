package com.jfaf.calendario.presentation.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.SharedAssignment
import com.jfaf.calendario.domain.repository.SocialError
import com.jfaf.calendario.domain.repository.SocialException
import com.jfaf.calendario.domain.repository.SocialRepository
import com.jfaf.calendario.domain.repository.TodayProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Calendario de solo lectura de un compañero: mes mostrado, sus turnos y el estado de la carga. */
data class SharedScheduleUiState(
    val alias: String,
    val month: CalendarMonth,
    val assignments: List<SharedAssignment> = emptyList(),
    val loading: Boolean = true,
    val error: SocialError? = null,
)

class SharedScheduleViewModel(
    private val ownerId: String,
    private val alias: String,
    private val social: SocialRepository,
    today: TodayProvider,
) : ViewModel() {

    private val state = MutableStateFlow(SharedScheduleUiState(alias, CalendarMonth.of(today.today())))
    val uiState: StateFlow<SharedScheduleUiState> = state.asStateFlow()

    init {
        load()
    }

    fun onPreviousMonth() = moveTo(state.value.month.previous)
    fun onNextMonth() = moveTo(state.value.month.next)

    /** Recarga el mes actual (p. ej. al volver a la pantalla). */
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
}
