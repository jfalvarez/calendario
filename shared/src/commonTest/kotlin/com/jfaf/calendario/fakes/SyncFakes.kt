package com.jfaf.calendario.fakes

import com.jfaf.calendario.data.sync.SyncStateStore
import com.jfaf.calendario.data.sync.SyncTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Estado de sincronización en memoria: cursores por tabla y cuenta, y la última cuenta usada. */
class InMemorySyncStateStore : SyncStateStore {
    private val cursors = mutableMapOf<Pair<String, String>, String>()
    private var last: String? = null

    override suspend fun cursor(table: String, userId: String): String? = cursors[table to userId]

    override suspend fun saveCursor(table: String, userId: String, cursor: String) {
        cursors[table to userId] = cursor
    }

    override suspend fun lastUserId(): String? = last

    override suspend fun saveLastUserId(userId: String) {
        last = userId
    }
}

/**
 * Tabla de prueba que anota sus llamadas en [events] (compartida entre tablas, para ver el orden).
 * [pullResult] es el cursor nuevo que devuelve al descargar.
 */
class RecordingSyncTable(
    override val name: String,
    private val events: MutableList<String>,
    private val pullResult: String? = null,
) : SyncTable {
    private val dirty = MutableStateFlow(0)

    /** Cuántas veces se ha vaciado la tabla localmente. */
    var cleared = 0
        private set

    override fun observeDirtyCount(): Flow<Int> = dirty

    override suspend fun push(userId: String) {
        events += "push:$name"
    }

    override suspend fun pull(userId: String, cursor: String?): String? {
        events += "pull:$name:${cursor ?: "-"}"
        return pullResult
    }

    override suspend fun clearLocal() {
        cleared++
        events += "clear:$name"
    }
}
