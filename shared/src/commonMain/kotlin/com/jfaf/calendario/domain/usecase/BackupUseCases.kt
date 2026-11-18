package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.backup.decodeBackupJson
import com.jfaf.calendario.domain.backup.encodeBackupJson
import com.jfaf.calendario.domain.backup.encodeNotesCsv
import com.jfaf.calendario.domain.backup.encodeShiftsCsv
import com.jfaf.calendario.domain.model.BackupSnapshot
import com.jfaf.calendario.domain.repository.BackupStore

/** Los tres ficheros de una exportación: copia completa para restaurar y dos hojas para leer en Excel. */
data class BackupFiles(val json: String, val shiftsCsv: String, val notesCsv: String)

class ExportBackupUseCase(private val store: BackupStore) {
    suspend operator fun invoke(exportedAt: String): BackupFiles {
        val snapshot = store.readAll()
        return BackupFiles(
            json = encodeBackupJson(snapshot, exportedAt),
            shiftsCsv = encodeShiftsCsv(snapshot),
            notesCsv = encodeNotesCsv(snapshot),
        )
    }
}

/**
 * Restaura una copia sustituyendo todos los datos actuales. Antes de llamar a `replaceAll`, valida el
 * fichero entero: si no es válido no se toca nada. Devuelve el resumen de lo importado.
 */
class ImportBackupUseCase(private val store: BackupStore) {
    suspend operator fun invoke(text: String): Result<BackupSnapshot> = runCatching {
        val snapshot = decodeBackupJson(text)
        store.replaceAll(snapshot)
        snapshot
    }
}
