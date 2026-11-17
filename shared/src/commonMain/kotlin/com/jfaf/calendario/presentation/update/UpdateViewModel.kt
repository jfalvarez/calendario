package com.jfaf.calendario.presentation.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.model.UpdateStatus
import com.jfaf.calendario.domain.usecase.CheckForUpdateUseCase
import com.jfaf.calendario.domain.usecase.DismissRecommendedUpdateUseCase
import com.jfaf.calendario.domain.usecase.ObserveDismissedUpdateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Estado de actualización que ve la UI. Comprueba al crearse y al volver a primer plano, como máximo una vez por
 * [minInterval]; un resultado `Unknown` (sin red, error) se reintenta en la siguiente ocasión.
 */
class UpdateViewModel(
    private val check: CheckForUpdateUseCase,
    observeDismissed: ObserveDismissedUpdateUseCase,
    private val dismissRecommended: DismissRecommendedUpdateUseCase,
    foregroundEvents: Flow<Unit>,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val minInterval: Duration = 1.hours,
) : ViewModel() {
    private val latest = MutableStateFlow<UpdateStatus>(UpdateStatus.Unknown)
    private val mutex = Mutex()
    private var lastCheck: TimeMark? = null
    private var lastWasUnknown = false

    /** Lo que debe pintarse: una recomendación ya descartada para esa versión se oculta. */
    val status: StateFlow<UpdateStatus> = combine(latest, observeDismissed()) { status, dismissed ->
        if (status is UpdateStatus.Recommended && status.policy.recommendedVersionCode <= dismissed) UpdateStatus.UpToDate else status
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UpdateStatus.Unknown)

    init {
        viewModelScope.launch { refresh() }
        viewModelScope.launch { foregroundEvents.collect { refresh() } }
    }

    private suspend fun refresh() = mutex.withLock {
        val last = lastCheck
        if (last != null && !lastWasUnknown && last.elapsedNow() < minInterval) return@withLock
        val result = check()
        lastCheck = timeSource.markNow()
        lastWasUnknown = result is UpdateStatus.Unknown
        // Sin respuesta no se "desbloquea" ni se bloquea: se conserva el último estado fiable.
        if (result !is UpdateStatus.Unknown) latest.value = result
    }

    fun onDismissRecommended() {
        val status = latest.value as? UpdateStatus.Recommended ?: return
        viewModelScope.launch { dismissRecommended(status.policy.recommendedVersionCode) }
    }
}
