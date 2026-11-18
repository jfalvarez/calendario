package com.jfaf.calendario.desktop

import com.jfaf.calendario.domain.repository.ErrorReporter
import io.sentry.Sentry
import java.util.Properties

/**
 * Informes de fallos con Sentry en escritorio. La clave sale de `sentry.properties` (generado desde `local.properties`);
 * vacía = desactivado. Mismas reglas de privacidad que en Android: sin datos personales.
 */
object CrashReporting {
    fun init(version: String) {
        val props = Properties()
        object {}.javaClass.classLoader.getResourceAsStream("sentry.properties")?.use { props.load(it) }
        val dsn = props.getProperty("dsn", "").trim()
        if (dsn.isEmpty()) return
        Sentry.init { options ->
            options.dsn = dsn
            options.release = "com.jfaf.calendario.desktop@$version"
            options.environment = "desktop"
            options.isSendDefaultPii = false
            options.isAttachServerName = false // evita enviar el nombre del equipo
        }
    }
}

class SentryErrorReporter : ErrorReporter {
    override fun report(error: Throwable, context: String) {
        if (!Sentry.isEnabled()) return
        Sentry.captureException(error) { scope -> scope.setTag("context", context) }
    }

    override val canSendTest: Boolean get() = Sentry.isEnabled()

    override fun sendTestReport() {
        Sentry.captureException(RuntimeException("Informe de prueba de NurseOClock (escritorio)"))
    }
}
