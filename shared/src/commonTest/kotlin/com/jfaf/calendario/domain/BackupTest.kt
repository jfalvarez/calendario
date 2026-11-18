package com.jfaf.calendario.domain

import com.jfaf.calendario.domain.backup.BackupFormatException
import com.jfaf.calendario.domain.backup.decodeBackupJson
import com.jfaf.calendario.domain.backup.encodeBackupJson
import com.jfaf.calendario.domain.backup.encodeNotesCsv
import com.jfaf.calendario.domain.backup.encodeShiftsCsv
import com.jfaf.calendario.domain.model.BackupAssignment
import com.jfaf.calendario.domain.model.BackupNote
import com.jfaf.calendario.domain.model.BackupShift
import com.jfaf.calendario.domain.model.BackupSnapshot
import com.jfaf.calendario.domain.model.NoteKind
import com.jfaf.calendario.domain.repository.BackupStore
import com.jfaf.calendario.domain.usecase.ExportBackupUseCase
import com.jfaf.calendario.domain.usecase.ImportBackupUseCase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BackupTest {
    private val day = LocalDate(2026, 11, 3)
    private val snapshot = BackupSnapshot(
        shifts = listOf(BackupShift("default-morning", "Mañana", "M", 0xFF2196F3, 0, 420, 900, 30)),
        assignments = listOf(BackupAssignment(day, 0, "default-morning")),
        holidays = listOf(LocalDate(2026, 12, 25)),
        notes = listOf(BackupNote("n1", day, NoteKind.IOwe, "Ana", "Cambio; urgente", false)),
    )

    @Test
    fun jsonRoundTripKeepsEverything() {
        val text = encodeBackupJson(snapshot, "2026-11-18T10:00:00Z")
        assertEquals(snapshot, decodeBackupJson(text))
    }

    @Test
    fun rejectsOtherFormatVersions() {
        val text = encodeBackupJson(snapshot, "x").replace("\"format\": 1", "\"format\": 99")
        assertFailsWith<BackupFormatException> { decodeBackupJson(text) }
    }

    @Test
    fun rejectsDuplicatedPositionOnSameDay() {
        val twice = snapshot.copy(assignments = snapshot.assignments + BackupAssignment(day, 0, "default-morning"))
        assertFailsWith<BackupFormatException> { decodeBackupJson(encodeBackupJson(twice, "x")) }
    }

    @Test
    fun rejectsAssignmentWithUnknownShift() {
        val broken = snapshot.copy(assignments = listOf(BackupAssignment(day, 0, "no-existe")))
        assertFailsWith<BackupFormatException> { decodeBackupJson(encodeBackupJson(broken, "x")) }
    }

    @Test
    fun rejectsGarbage() {
        assertFailsWith<BackupFormatException> { decodeBackupJson("esto no es json") }
    }

    @Test
    fun csvSheetsUseSemicolonsAndQuoteCells() {
        val shifts = encodeShiftsCsv(snapshot)
        assertTrue(shifts.startsWith("Fecha;Posición;Turno"))
        assertTrue(shifts.contains("2026-11-03;1;Mañana"))
        val notes = encodeNotesCsv(snapshot)
        assertTrue(notes.contains("\"Cambio; urgente\""))
    }

    @Test
    fun exportAndImportThroughTheStore() = runTest {
        val store = MemoryBackupStore(snapshot)
        val files = ExportBackupUseCase(store)("2026-11-18T10:00:00Z")
        val other = MemoryBackupStore(BackupSnapshot(emptyList(), emptyList(), emptyList(), emptyList()))
        val result = ImportBackupUseCase(other)(files.json)
        assertTrue(result.isSuccess)
        assertEquals(snapshot, other.current)
    }

    @Test
    fun invalidFileLeavesTheStoreUntouched() = runTest {
        val store = MemoryBackupStore(snapshot)
        val result = ImportBackupUseCase(store)("basura")
        assertTrue(result.isFailure)
        assertEquals(snapshot, store.current)
    }
}

private class MemoryBackupStore(var current: BackupSnapshot) : BackupStore {
    override suspend fun readAll() = current
    override suspend fun replaceAll(snapshot: BackupSnapshot) { current = snapshot }
}
