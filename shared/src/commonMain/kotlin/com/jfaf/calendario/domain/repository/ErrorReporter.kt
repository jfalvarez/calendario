package com.jfaf.calendario.domain.repository

/**
 * Informes de fallos. Los cierres inesperados los recoge cada plataforma por su cuenta; esto sirve para avisar de
 * errores que la app captura y de los que conviene enterarse (p. ej. una sincronización que falla por algo que no es la red).
 * Nunca debe recibir datos personales (turnos, notas, correos…): sólo la excepción y un texto de contexto fijo.
 */
interface ErrorReporter {
    fun report(error: Throwable, context: String)

    /** `true` si en esta compilación se puede mandar un informe de prueba desde Ajustes (no en la versión publicada). */
    val canSendTest: Boolean

    fun sendTestReport()
}

/** Sin informes (clave no configurada o pruebas). */
object NoOpErrorReporter : ErrorReporter {
    override fun report(error: Throwable, context: String) = Unit
    override val canSendTest = false
    override fun sendTestReport() = Unit
}
