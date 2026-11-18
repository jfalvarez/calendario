package com.jfaf.calendario.presentation.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.model.AuthState
import com.jfaf.calendario.domain.model.LinkRequestResult
import com.jfaf.calendario.domain.model.LinkSummary
import com.jfaf.calendario.domain.model.PendingRequest
import com.jfaf.calendario.domain.model.SwapProposal
import com.jfaf.calendario.domain.model.isValidAlias
import com.jfaf.calendario.domain.repository.SocialError
import com.jfaf.calendario.domain.repository.SocialException
import com.jfaf.calendario.domain.repository.SocialRepository
import com.jfaf.calendario.domain.usecase.ObserveAuthStateUseCase
import com.jfaf.calendario.presentation.update.AppForegroundEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la sección "Compañeros": mi alias, solicitudes que me esperan y vínculos. */
data class SocialUiState(
    val myAlias: String? = null,
    /** Solicitudes pendientes de mi respuesta (banner de la pantalla principal). */
    val pending: List<PendingRequest> = emptyList(),
    val links: List<LinkSummary> = emptyList(),
    /** Propuestas de cambio de turno: las que me han hecho y las mías. */
    val swaps: List<SwapProposal> = emptyList(),
    val busy: Boolean = false,
    /** Error de la última acción; null si fue bien. */
    val error: SocialError? = null,
    /** Resultado de la última solicitud por alias, para mostrarlo bajo el campo. */
    val requestResult: LinkRequestResult? = null,
    val aliasSaved: Boolean = false,
    /** El texto escrito no cumple el formato del alias: no se envía nada al servidor. */
    val invalidAliasInput: Boolean = false,
)

class SocialViewModel(
    private val social: SocialRepository,
    observeAuth: ObserveAuthStateUseCase,
    foregroundEvents: AppForegroundEvents,
) : ViewModel() {

    private val state = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = state.asStateFlow()

    init {
        // Sin notificaciones push, las solicitudes se ven al iniciar sesión y al volver a primer plano.
        observeAuth().filterIsInstance<AuthState.SignedIn>().onEach { refresh() }.launchIn(viewModelScope)
        foregroundEvents.events.onEach { refresh() }.launchIn(viewModelScope)
    }

    /** Recarga alias, solicitudes pendientes y vínculos. Sin sesión, los fallos dejan las listas vacías. */
    fun refresh() {
        viewModelScope.launch {
            val alias = social.myAlias().getOrNull()
            val pending = social.pendingRequests().getOrDefault(emptyList())
            val links = social.myLinks().getOrDefault(emptyList())
            val swaps = social.mySwaps().getOrDefault(emptyList())
            state.update { it.copy(myAlias = alias, pending = pending, links = links, swaps = swaps) }
        }
    }

    fun saveAlias(input: String) {
        val alias = input.trim()
        if (!isValidAlias(alias)) {
            state.update { it.copy(invalidAliasInput = true, aliasSaved = false, error = null) }
            return
        }
        perform {
            social.setAlias(alias).map { saved ->
                state.update { it.copy(myAlias = saved, aliasSaved = true, invalidAliasInput = false) }
            }
        }
    }

    fun requestLink(input: String) {
        val alias = input.trim()
        if (!isValidAlias(alias)) {
            state.update { it.copy(invalidAliasInput = true, requestResult = null, error = null) }
            return
        }
        perform {
            social.requestLink(alias).map { result ->
                state.update { it.copy(requestResult = result, invalidAliasInput = false) }
            }
        }
    }

    fun accept(linkId: String) = perform { social.acceptRequest(linkId) }
    fun reject(linkId: String) = perform { social.rejectRequest(linkId) }
    fun block(linkId: String) = perform { social.blockRequester(linkId) }
    fun revoke(linkId: String) = perform { social.revokeLink(linkId) }

    /** Aceptar o rechazar una propuesta que me han hecho. Al aceptar, el servidor cambia los dos calendarios. */
    fun respondSwap(swapId: String, accept: Boolean) = perform { social.respondSwap(swapId, accept) }
    fun cancelSwap(swapId: String) = perform { social.cancelSwap(swapId) }

    /** Borra avisos de la última acción, por ejemplo al volver a escribir en el campo. */
    fun clearMessages() = state.update {
        it.copy(error = null, requestResult = null, aliasSaved = false, invalidAliasInput = false)
    }

    /** Ejecuta una acción contra el servidor, muestra su error y recarga las listas si fue bien. */
    private fun perform(action: suspend () -> Result<Unit>) {
        if (state.value.busy) return
        state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val result = action()
            state.update { it.copy(busy = false, error = result.exceptionOrNull()?.let(::errorOf)) }
            if (result.isSuccess) refresh()
        }
    }

    private fun errorOf(e: Throwable): SocialError = (e as? SocialException)?.error ?: SocialError.Unknown
}
