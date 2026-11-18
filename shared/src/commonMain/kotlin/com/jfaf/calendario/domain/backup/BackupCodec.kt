package com.jfaf.calendario.domain.backup

import com.jfaf.calendario.domain.model.BackupAssignment
import com.jfaf.calendario.domain.model.BackupNote
import com.jfaf.calendario.domain.model.BackupShift
import com.jfaf.calendario.domain.model.BackupSnapshot
import com.jfaf.calendario.domain.model.NoteKind
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Versión del formato del fichero. Si cambia, la importación rechaza las copias de otra versión. */
const val BACKUP_FORMAT = 1

/** Límites de tamaño al importar: un fichero fuera de estos rangos no es una copia de esta app. */
private const val MAX_NOTE_TEXT = 1000
private const val MAX_ROWS = 100_000

class BackupFormatException(message: String) : Exception(message)

@Serializable
internal data class BackupDto(
    val format: Int,
    val exportedAt: String,
    val shifts: List<ShiftDto> = emptyList(),
    val assignments: List<AssignmentDto> = emptyList(),
    val holidays: List<String> = emptyList(),
    val notes: List<NoteDto> = emptyList(),
)

@Serializable
internal data class ShiftDto(
    val id: String,
    val name: String,
    val abbreviation: String,
    val colorArgb: Long,
    val sortOrder: Int,
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    val breakMinutes: Int = 0,
)

@Serializable
internal data class AssignmentDto(val date: String, val slot: Int, val shiftId: String)

@Serializable
internal data class NoteDto(
    val id: String,
    val date: String,
    val kind: String,
    val person: String? = null,
    val text: String,
    val settled: Boolean = false,
)

private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = true }

/** Convierte la copia a JSON. `exportedAt` es la fecha de exportación en ISO-8601, la pone quien llama. */
fun encodeBackupJson(snapshot: BackupSnapshot, exportedAt: String): String {
    val dto = BackupDto(
        format = BACKUP_FORMAT,
        exportedAt = exportedAt,
        shifts = snapshot.shifts.map { ShiftDto(it.id, it.name, it.abbreviation, it.colorArgb, it.sortOrder, it.startMinutes, it.endMinutes, it.breakMinutes) },
        assignments = snapshot.assignments.map { AssignmentDto(it.date.toString(), it.slot, it.shiftId) },
        holidays = snapshot.holidays.map { it.toString() },
        notes = snapshot.notes.map { NoteDto(it.id, it.date.toString(), it.kind.name, it.person, it.text, it.settled) },
    )
    return json.encodeToString(BackupDto.serializer(), dto)
}

/**
 * Lee y valida una copia. Rechaza formatos desconocidos, filas fuera de rango, asignaciones duplicadas
 * en la misma posición y turnos que no están en la copia: así no se importa nada incoherente.
 */
fun decodeBackupJson(text: String): BackupSnapshot {
    val dto = try {
        json.decodeFromString(BackupDto.serializer(), text)
    } catch (e: Exception) {
        throw BackupFormatException("El fichero no es una copia de seguridad válida.")
    }
    if (dto.format != BACKUP_FORMAT) throw BackupFormatException("Versión de copia no compatible (${dto.format}).")
    val total = dto.shifts.size + dto.assignments.size + dto.holidays.size + dto.notes.size
    if (total > MAX_ROWS) throw BackupFormatException("La copia tiene demasiados datos.")

    val shifts = dto.shifts.map { BackupShift(it.id, it.name, it.abbreviation, it.colorArgb, it.sortOrder, it.startMinutes, it.endMinutes, it.breakMinutes) }
    val shiftIds = shifts.map { it.id }.toSet()
    if (shiftIds.size != shifts.size) throw BackupFormatException("Hay turnos repetidos en la copia.")

    val assignments = dto.assignments.map {
        if (it.slot !in 0..1) throw BackupFormatException("Posición de turno no válida (${it.slot}).")
        if (it.shiftId !in shiftIds) throw BackupFormatException("Un turno asignado no existe en la copia.")
        BackupAssignment(parseDate(it.date), it.slot, it.shiftId)
    }
    if (assignments.map { it.date to it.slot }.toSet().size != assignments.size) {
        throw BackupFormatException("Hay dos turnos en la misma posición del mismo día.")
    }

    val notes = dto.notes.map {
        if (it.text.length > MAX_NOTE_TEXT) throw BackupFormatException("Una nota es demasiado larga.")
        val kind = runCatching { NoteKind.valueOf(it.kind) }.getOrElse { throw BackupFormatException("Tipo de nota no válido.") }
        BackupNote(it.id, parseDate(it.date), kind, it.person, it.text, it.settled)
    }

    return BackupSnapshot(
        shifts = shifts,
        assignments = assignments,
        holidays = dto.holidays.map { parseDate(it) },
        notes = notes,
    )
}

/** CSV para Excel: hoja de turnos por día. Separador `;` (Excel en español lo abre bien). */
fun encodeShiftsCsv(snapshot: BackupSnapshot): String {
    val names = snapshot.shifts.associate { it.id to it.name }
    val rows = snapshot.assignments.sortedWith(compareBy({ it.date }, { it.slot })).map {
        listOf(it.date.toString(), (it.slot + 1).toString(), names[it.shiftId].orEmpty())
    }
    return csv(listOf("Fecha", "Posición", "Turno"), rows)
}

/** CSV para Excel: hoja de notas y deudas. */
fun encodeNotesCsv(snapshot: BackupSnapshot): String {
    val rows = snapshot.notes.sortedBy { it.date }.map {
        listOf(it.date.toString(), it.kind.name, it.person.orEmpty(), it.text, if (it.settled) "sí" else "no")
    }
    return csv(listOf("Fecha", "Tipo", "Persona", "Texto", "Saldada"), rows)
}

private fun csv(header: List<String>, rows: List<List<String>>): String =
    (listOf(header) + rows).joinToString("\r\n") { line -> line.joinToString(";") { csvCell(it) } } + "\r\n"

private fun csvCell(value: String): String =
    if (value.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

private fun parseDate(value: String): LocalDate =
    try { LocalDate.parse(value) } catch (e: Exception) { throw BackupFormatException("Fecha no válida en la copia ($value).") }
