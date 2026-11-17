package com.jfaf.calendario.domain.repository

import com.jfaf.calendario.domain.model.AppVersionPolicy

interface AppConfigRepository {
    /** Política vigente para esta plataforma, o `null` si no está disponible. Puede lanzar si falla la red. */
    suspend fun fetchPolicy(): AppVersionPolicy?
}
