package com.jfaf.calendario.presentation.update

import com.jfaf.calendario.domain.model.AppVersionPolicy

/** Inicia la actualización en la plataforma (por ahora: abrir la ficha de la tienda o la página de descarga). */
fun interface UpdateLauncher {
    fun startUpdate(policy: AppVersionPolicy)
}
