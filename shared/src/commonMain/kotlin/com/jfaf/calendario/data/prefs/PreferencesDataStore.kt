package com.jfaf.calendario.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toPath

/** Archivo de ajustes (debe acabar en `.preferences_pb`). */
const val USER_PREFERENCES_FILE = "user_prefs.preferences_pb"

/**
 * DataStore de ajustes a partir de una ruta absoluta que aporta cada plataforma.
 * Sólo puede haber uno activo por archivo: la app lo crea como singleton.
 */
fun createPreferencesDataStore(
    path: String,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(scope = scope, produceFile = { path.toPath() })
