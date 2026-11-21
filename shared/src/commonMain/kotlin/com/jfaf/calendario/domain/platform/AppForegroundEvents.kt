package com.jfaf.calendario.domain.platform

import kotlinx.coroutines.flow.SharedFlow

/** Avisos de que la app vuelve a primer plano. Cada plataforma los emite; la app los escucha sin conocer cuál es. */
interface AppForegroundEvents {
    val events: SharedFlow<Unit>
}
