package com.jfaf.calendario.data.sync

/** Siembra los datos por defecto (p. ej. los turnos de ejemplo) tras vaciar la base local. */
fun interface DefaultDataSeeder {
    suspend fun seed()
}

/**
 * Si en este dispositivo entra una cuenta distinta de la última, se vacían los datos locales (eran de otro
 * usuario) para no subirlos a la cuenta nueva. La primera cuenta que inicia sesión adopta los datos locales.
 */
class AccountAdoption(
    private val tables: List<SyncTable>,
    private val state: SyncStateStore,
    private val seeder: DefaultDataSeeder,
) {
    suspend fun adopt(userId: String) {
        val last = state.lastUserId()
        if (last != null && last != userId) {
            tables.forEach { it.clearLocal() }
            seeder.seed()
        }
        if (last != userId) state.saveLastUserId(userId)
    }
}
