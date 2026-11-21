package com.jfaf.calendario.data.sync

import kotlinx.coroutines.flow.Flow

/**
 * Una tabla local que se sincroniza con el servidor. El motor no conoce las entidades: sólo llama a esto.
 * Para sincronizar una entidad nueva basta con una clase que la implemente y añadirla en `defaultSyncTables`.
 */
interface SyncTable {
    /** Nombre estable de la tabla: forma parte de la clave del cursor guardado en el dispositivo. */
    val name: String

    /** Filas con cambios locales pendientes de subir. */
    fun observeDirtyCount(): Flow<Int>

    /** Sube las filas modificadas localmente y las marca como limpias. */
    suspend fun push(userId: String)

    /** Descarga y fusiona las novedades desde [cursor]. Devuelve el cursor nuevo, o `null` si no ha cambiado. */
    suspend fun pull(userId: String, cursor: String?): String?

    /** Borra los datos locales de esta tabla (al cambiar de cuenta). */
    suspend fun clearLocal()
}
