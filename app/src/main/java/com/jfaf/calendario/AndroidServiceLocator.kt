package com.jfaf.calendario

import org.koin.core.context.GlobalContext
import org.koin.core.qualifier.Qualifier

/**
 * Único punto donde los componentes de Android (receivers y widget) alcanzan Koin. Android crea esas clases por su
 * cuenta y no admite constructores con parámetros, así que no pueden recibir sus dependencias. Usar siempre este
 * objeto y no `GlobalContext` directamente.
 */
object AndroidServiceLocator {
    inline fun <reified T : Any> get(qualifier: Qualifier? = null): T = GlobalContext.get().get(qualifier = qualifier)
}
