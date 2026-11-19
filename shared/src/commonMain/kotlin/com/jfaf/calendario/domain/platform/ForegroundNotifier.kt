package com.jfaf.calendario.domain.platform

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Implementación de [AppForegroundEvents] que emiten las plataformas (Android: proceso en ON_START;
 * escritorio: foco de ventana).
 */
class ForegroundNotifier : AppForegroundEvents {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val events: SharedFlow<Unit> = _events

    fun notifyForeground() {
        _events.tryEmit(Unit)
    }
}
