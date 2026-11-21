package com.jfaf.calendario.crash

import android.app.Application
import com.jfaf.calendario.BuildConfig
import com.jfaf.calendario.domain.repository.ErrorReporter
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid

/**
 * Informes de fallos con Sentry. Sin `sentry.dsn` en `local.properties` queda desactivado (no se envía nada).
 * Privacidad: sin datos personales, sin capturas de pantalla ni jerarquía de vistas, sin rastro de toques y sin trazas
 * de rendimiento; sólo la excepción, el dispositivo y la versión de la app.
 */
object CrashReporting {
    fun init(app: Application) {
        if (BuildConfig.SENTRY_DSN.isBlank()) return
        SentryAndroid.init(app) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.release = "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
            options.environment = BuildConfig.BUILD_TYPE // debug, releaseCheck o release
            options.isSendDefaultPii = false
            options.isAttachScreenshot = false
            options.isAttachViewHierarchy = false
            options.isEnableUserInteractionBreadcrumbs = false
            options.isAttachServerName = false
            options.isDebug = BuildConfig.BUILD_TYPE != "release" // en pruebas, el SDK explica en logcat lo que envía
        }
    }
}

class SentryErrorReporter : ErrorReporter {
    override fun report(error: Throwable, context: String) {
        if (!Sentry.isEnabled()) return
        Sentry.captureException(error) { scope -> scope.setTag("context", context) }
    }
}
