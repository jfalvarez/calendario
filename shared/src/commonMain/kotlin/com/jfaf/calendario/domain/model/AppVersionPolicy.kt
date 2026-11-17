package com.jfaf.calendario.domain.model

/** Política de versiones que publica el servidor para una plataforma (tabla `app_config`). */
data class AppVersionPolicy(
    /** Por debajo de esta versión la app debe actualizarse para seguir usándose. */
    val minVersionCode: Int,
    /** Por debajo de esta versión se recomienda actualizar, sin obligar. */
    val recommendedVersionCode: Int,
    val message: String? = null,
    val storeUrl: String? = null,
)
