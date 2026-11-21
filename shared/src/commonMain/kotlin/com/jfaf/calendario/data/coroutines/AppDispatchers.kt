package com.jfaf.calendario.data.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Dispatchers que usa la capa de datos. Inyectarlos permite que las pruebas los sustituyan
 * (p. ej. por un `StandardTestDispatcher`) en vez de depender de los fijos de `Dispatchers`.
 */
interface AppDispatchers {
    /** Acceso a disco y base de datos. */
    val io: CoroutineDispatcher

    /** Trabajo de CPU y de fondo (sincronización, recordatorios, widgets). */
    val default: CoroutineDispatcher
}

/** Implementación real de la app. */
object DefaultAppDispatchers : AppDispatchers {
    override val io: CoroutineDispatcher get() = Dispatchers.IO
    override val default: CoroutineDispatcher get() = Dispatchers.Default
}
