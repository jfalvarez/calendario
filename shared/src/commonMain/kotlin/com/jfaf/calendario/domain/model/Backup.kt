package com.jfaf.calendario.domain.model

import kotlinx.datetime.LocalDate

/** Copia de seguridad de los datos locales del usuario: turnos, asignaciones, festivos y notas. */
data class BackupSnapshot(
    val shifts: List<BackupShift>,
    val assignments: List<BackupAssignment>,
    val holidays: List<LocalDate>,
    val notes: List<BackupNote>,
)

data class BackupShift(
    val id: String,
    val name: String,
    val abbreviation: String,
    val colorArgb: Long,
    val sortOrder: Int,
    val startMinutes: Int?,
    val endMinutes: Int?,
    val breakMinutes: Int,
)

/** Un turno en una posición de un día (`slot` 0 o 1). */
data class BackupAssignment(val date: LocalDate, val slot: Int, val shiftId: String)

data class BackupNote(
    val id: String,
    val date: LocalDate,
    val kind: NoteKind,
    val person: String?,
    val text: String,
    val settled: Boolean,
)
