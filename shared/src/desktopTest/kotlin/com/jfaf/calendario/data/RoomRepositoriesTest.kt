package com.jfaf.calendario.data

import androidx.room.Room
import com.jfaf.calendario.data.local.CalendarDatabase
import com.jfaf.calendario.data.local.buildCalendarDatabase
import com.jfaf.calendario.data.repository.RoomShiftAssignmentRepository
import com.jfaf.calendario.data.repository.RoomShiftRepository
import com.jfaf.calendario.domain.model.Shift
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomRepositoriesTest {
    private val file = File.createTempFile("calendario-test", ".db")
    private val db = buildCalendarDatabase(Room.databaseBuilder<CalendarDatabase>(file.absolutePath))
    private val shifts = RoomShiftRepository(db.shiftDao())
    private val assignments = RoomShiftAssignmentRepository(db.shiftAssignmentDao())
    private val day = LocalDate(2025, 11, 12)

    @AfterTest
    fun tearDown() {
        db.close()
        file.delete()
    }

    @Test
    fun seedsDefaultShiftsOnCreation() = runTest {
        assertEquals(listOf("M", "T", "N"), shifts.observeShifts().first().map { it.abbreviation })
    }

    @Test
    fun upsertAddsAndUpdatesShift() = runTest {
        shifts.upsert(Shift("x1", "Refuerzo", "R", 0xFF00FF00))
        shifts.upsert(Shift("x1", "Refuerzo 2", "R2", 0xFF00FF00))
        val list = shifts.observeShifts().first()
        assertEquals(4, list.size)
        assertEquals("Refuerzo 2", list.last().name)
    }

    @Test
    fun assignmentsRoundTripAndLimitToTwo() = runTest {
        assignments.setShiftIds(day, listOf("default-morning", "default-night", "default-afternoon"))
        assertEquals(listOf("default-morning", "default-night"), assignments.getShiftIds(day))
        val inRange = assignments.observeAssignments(day, day).first()
        assertEquals(mapOf(day to listOf("default-morning", "default-night")), inRange)
    }

    @Test
    fun clearingMarksDeletedAndHidesTheDay() = runTest {
        assignments.setShiftIds(day, listOf("default-morning"))
        assignments.setShiftIds(day, emptyList())
        assertTrue(assignments.getShiftIds(day).isEmpty())
        assertTrue(assignments.observeAssignments(day, day).first().isEmpty())
        // reutiliza la fila borrada al volver a pintar
        assignments.setShiftIds(day, listOf("default-night"))
        assertEquals(listOf("default-night"), assignments.getShiftIds(day))
    }

    @Test
    fun scheduleFieldsRoundTripAndDefaultsHaveHours() = runTest {
        assertEquals(480, shifts.observeShifts().first().first().workMinutes) // Mañana 06:00-14:00
        shifts.upsert(Shift("x2", "Partido", "P", 0xFF123456, 8 * 60, 16 * 60, 30))
        val saved = shifts.observeShifts().first().single { it.id == "x2" }
        assertEquals(450, saved.workMinutes)
    }

    @Test
    fun deleteHidesShiftAndReorderPersists() = runTest {
        shifts.delete("default-afternoon")
        assertEquals(listOf("default-morning", "default-night"), shifts.observeShifts().first().map { it.id })
        shifts.reorder(listOf("default-night", "default-morning"))
        assertEquals(listOf("default-night", "default-morning"), shifts.observeShifts().first().map { it.id })
    }

    @Test
    fun bulkSetAndGetAssignments() = runTest {
        val d2 = LocalDate(2025, 11, 13)
        assignments.setAssignments(mapOf(day to listOf("default-morning"), d2 to listOf("default-night", "default-afternoon")))
        assertEquals(
            mapOf(day to listOf("default-morning"), d2 to listOf("default-night", "default-afternoon")),
            assignments.getAssignments(day, d2),
        )
        assignments.setAssignments(mapOf(day to emptyList(), d2 to listOf("default-night")))
        assertEquals(mapOf(d2 to listOf("default-night")), assignments.getAssignments(day, d2))
    }

}
