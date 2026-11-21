package com.jfaf.calendario.presentation.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.model.AuthState
import com.jfaf.calendario.domain.model.BlockedUser
import com.jfaf.calendario.domain.model.LinkRequestResult
import com.jfaf.calendario.domain.model.LinkSummary
import com.jfaf.calendario.domain.model.PendingRequest
import com.jfaf.calendario.domain.model.SwapProposal
import com.jfaf.calendario.domain.platform.AppForegroundEvents
import com.jfaf.calendario.domain.repository.SocialError
import com.jfaf.calendario.domain.repository.toSocialError
import com.jfaf.calendario.domain.usecase.LoadSocialOverviewUseCase
import com.jfaf.calendario.domain.usecase.ManageLinksUseCase
import com.jfaf.calendario.domain.usecase.ManageSwapsUseCase
import com.jfaf.calendario.domain.usecase.ObserveAuthStateUseCase
import com.jfaf.calendario.domain.usecase.RequestLinkUseCase
import com.jfaf.calendario.domain.usecase.SaveAliasUseCase
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
    /** Personas bloqueadas por mí; se pueden desbloquear desde Ajustes. */
    val blocked: List<BlockedUser> = emptyList(),
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
    private val loadOverview: LoadSocialOverviewUseCase,
    private val saveAliasUseCase: SaveAliasUseCase,
    private val requestLinkUseCase: RequestLinkUseCase,
    private val links: ManageLinksUseCase,
    private val swaps: ManageSwapsUseCase,
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

    /** Recarga alias, solicitudes pendientes, vínculos, propuestas y bloqueos. Sin sesión, las listas quedan vacías. */
    fun refresh() {
        viewModelScope.launch {
            val overview = loadOverview()
            state.update {
                it.copy(
                    myAlias = overview.alias,
                    pending = overview.pending,
                    links = overview.links,
                    swaps = overview.swaps,
                    blocked = overview.blocked,
                )
            }
        }
    }

    fun saveAlias(input: String) = perform(onInvalidAlias = {
        state.update { it.copy(invalidAliasInput = true, aliasSaved = false, error = null) }
    }) {
        saveAliasUseCase(input).map { saved ->
            state.update { it.copy(myAlias = saved, aliasSaved = true, invalidAliasInput = false) }
        }
    }

    fun requestLink(input: String) = perform(onInvalidAlias = {
        state.update { it.copy(invalidAliasInput = true, requestResult = null, error = null) }
    }) {
        requestLinkUseCase(input).map { result ->
            state.update { it.copy(requestResult = result, invalidAliasInput = false) }
        }
    }

    fun accept(linkId: String) = perform { links.accept(linkId) }
    fun reject(linkId: String) = perform { links.reject(linkId) }
    fun block(linkId: String) = perform { links.block(linkId) }
    fun unblock(blockedId: String) = perform { links.unblock(blockedId) }
    fun revoke(linkId: String) = perform { links.revoke(linkId) }

    /** Aceptar o rechazar una propuesta que me han hecho. Al aceptar, el servidor cambia los dos calendarios. */
    fun respondSwap(swapId: String, accept: Boolean) = perform { swaps.respond(swapId, accept) }
    fun cancelSwap(swapId: String) = perform { swaps.cancel(swapId) }

    /** Borra avisos de la última acción, por ejemplo al volver a escribir en el campo. */
    fun clearMessages() = state.update {
        it.copy(error = null, requestResult = null, aliasSaved = false, invalidAliasInput = false)
    }

    /**
     * Ejecuta una acción contra el servidor, muestra su error y recarga las listas si fue bien.
     * Un alias con formato inválido no es un error de servidor: se avisa con [onInvalidAlias].
     */
    private fun perform(onInvalidAlias: (() -> Unit)? = null, action: suspend () -> Result<Unit>) {
        if (state.value.busy) return
        state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val result = action()
            val error = result.exceptionOrNull()?.toSocialError()
            if (error == SocialError.InvalidAlias && onInvalidAlias != null) {
                state.update { it.copy(busy = false) }
                onInvalidAlias()
                return@launch
            }
            state.update { it.copy(busy = false, error = error) }
            if (result.isSuccess) refresh()
        }
    }
}
