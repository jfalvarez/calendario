package com.jfaf.calendario.presentation.update

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** Cada plataforma avisa aquí cuando la app vuelve a primer plano (Android: proceso en ON_START; escritorio: foco de ventana). */
class AppForegroundEvents {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events

    fun notifyForeground() {
        _events.tryEmit(Unit)
    }
}
