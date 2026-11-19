package com.jfaf.calendario.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/** Estado de sincronización que sobrevive al cierre de la app: un cursor por tabla y cuenta, y la última cuenta. */
interface SyncStateStore {
    suspend fun cursor(table: String, userId: String): String?
    suspend fun saveCursor(table: String, userId: String, cursor: String)
    suspend fun lastUserId(): String?
    suspend fun saveLastUserId(userId: String)
}

/** Implementación sobre DataStore de ajustes. Las claves no cambian: los cursores existentes siguen válidos. */
class DataStoreSyncStateStore(private val prefs: DataStore<Preferences>) : SyncStateStore {
    override suspend fun cursor(table: String, userId: String): String? =
        prefs.data.first()[stringPreferencesKey("sync_cursor_${table}_$userId")]

    override suspend fun saveCursor(table: String, userId: String, cursor: String) {
        prefs.edit { it[stringPreferencesKey("sync_cursor_${table}_$userId")] = cursor }
    }

    override suspend fun lastUserId(): String? = prefs.data.first()[LAST_USER_KEY]

    override suspend fun saveLastUserId(userId: String) {
        prefs.edit { it[LAST_USER_KEY] = userId }
    }

    private companion object {
        val LAST_USER_KEY = stringPreferencesKey("sync_last_user_id")
    }
}
