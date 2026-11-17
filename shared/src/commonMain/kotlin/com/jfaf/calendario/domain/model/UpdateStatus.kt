package com.jfaf.calendario.domain.model

/** Resultado de comparar la versión instalada con la política del servidor. */
sealed interface UpdateStatus {
    data object UpToDate : UpdateStatus

    /** No se pudo comprobar (sin red, error, sin política): la app arranca con normalidad. */
    data object Unknown : UpdateStatus

    data class Recommended(val policy: AppVersionPolicy) : UpdateStatus
    data class Required(val policy: AppVersionPolicy) : UpdateStatus
}
