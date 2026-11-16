package com.jfaf.calendario.data.local

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [ShiftEntity::class, ShiftAssignmentEntity::class],
    version = 2, // 2: horario del turno (inicio, fin, descanso)
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@ConstructedBy(CalendarDatabaseConstructor::class)
abstract class CalendarDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao
    abstract fun shiftAssignmentDao(): ShiftAssignmentDao
}

/** Room genera la implementación (`actual`) en cada plataforma con KSP. */
@Suppress("KotlinNoActualForExpect")
expect object CalendarDatabaseConstructor : RoomDatabaseConstructor<CalendarDatabase> {
    override fun initialize(): CalendarDatabase
}

/** Nombre del archivo de la base de datos. */
const val CALENDAR_DATABASE_NAME = "calendario.db"

/**
 * Configuración común. Cada plataforma sólo aporta el builder con la ruta:
 * Android `Room.databaseBuilder(context, ruta)` y escritorio `Room.databaseBuilder(ruta)`.
 */
fun buildCalendarDatabase(builder: RoomDatabase.Builder<CalendarDatabase>): CalendarDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addCallback(DefaultShiftsCallback)
        .build()

/**
 * Turnos de ejemplo al crear la base de datos. Los ids son **fijos** para que dos dispositivos que
 * creen su base de forma independiente no dupliquen los turnos al sincronizar; `updatedAt = 0` hace
 * que cualquier edición real gane.
 */
private object DefaultShiftsCallback : RoomDatabase.Callback() {
    override fun onCreate(connection: SQLiteConnection) {
        listOf(
            Seed("default-morning", "Mañana", "M", 0xFF2196F3L, 6 * 60, 14 * 60),
            Seed("default-afternoon", "Tarde", "T", 0xFFFF9800L, 14 * 60, 22 * 60),
            Seed("default-night", "Noche", "N", 0xFF3F51B5L, 22 * 60, 6 * 60),
        ).forEachIndexed { index, s ->
            connection.execSQL(
                "INSERT INTO shifts (id, name, abbreviation, colorArgb, sortOrder, updatedAt, deletedAt, dirty, startMinutes, endMinutes, breakMinutes) " +
                    "VALUES ('${s.id}', '${s.name}', '${s.abbreviation}', ${s.color}, $index, 0, NULL, 1, ${s.start}, ${s.end}, 0)"
            )
        }
    }
}

private data class Seed(val id: String, val name: String, val abbreviation: String, val color: Long, val start: Int, val end: Int)
