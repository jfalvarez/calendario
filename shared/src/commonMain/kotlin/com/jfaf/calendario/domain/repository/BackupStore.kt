package com.jfaf.calendario.domain.repository

import com.jfaf.calendario.domain.model.BackupSnapshot

/**
 * Acceso a los datos locales para la copia de seguridad. `replaceAll` sustituye todo lo que hay:
 * lo anterior se marca como borrado (para que la sincronización lo quite del servidor) y lo nuevo se sube.
 */
interface BackupStore {
    suspend fun readAll(): BackupSnapshot
    suspend fun replaceAll(snapshot: BackupSnapshot)
}
