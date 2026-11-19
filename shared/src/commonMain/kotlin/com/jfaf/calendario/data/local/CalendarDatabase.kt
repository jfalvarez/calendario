package com.jfaf.calendario.data.local

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.jfaf.calendario.data.coroutines.AppDispatchers
import com.jfaf.calendario.data.coroutines.DefaultAppDispatchers

@Database(
    entities = [ShiftEntity::class, ShiftAssignmentEntity::class, LocalHolidayEntity::class, DayNoteEntity::class],
    version = 6, // 2: horario del turno; 3: pago (reservado); 4: ids de asignación deterministas;  5: festivos locales; 6: notas y deudas por día
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 4, to = 5), AutoMigration(from = 5, to = 6)],
)
@ConstructedBy(CalendarDatabaseConstructor::class)
abstract class CalendarDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao
    abstract fun shiftAssignmentDao(): ShiftAssignmentDao
    abstract fun localHolidayDao(): LocalHolidayDao
    abstract fun dayNoteDao(): DayNoteDao
}

/** Room genera la implementación (`actual`) en cada plataforma con KSP. */
@Suppress("KotlinNoActualForExpect")
expect object CalendarDatabaseConstructor : RoomDatabaseConstructor<CalendarDatabase> {
    override fun initialize(): CalendarDatabase
}

/** Nombre del archivo de la base de datos. */
const val CALENDAR_DATABASE_NAME = "calendario.db"

/**
 * v3 → v4: el id de una asignación pasa a ser `fecha#posición` (p. ej. `2026-11-16#0`).
 * Así dos dispositivos que pintan el mismo día generan **la misma fila** y el servidor la resuelve
 * por «último cambio gana», en vez de chocar con el índice único (fecha, posición).
 */
private val Migration3To4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("UPDATE shift_assignments SET id = date || '#' || slot")
    }
}

/**
 * Configuración común. Cada plataforma sólo aporta el builder con la ruta:
 * Android `Room.databaseBuilder(context, ruta)` y escritorio `Room.databaseBuilder(ruta)`.
 */
fun buildCalendarDatabase(
    builder: RoomDatabase.Builder<CalendarDatabase>,
    dispatchers: AppDispatchers = DefaultAppDispatchers,
): CalendarDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(dispatchers.io)
        .addMigrations(Migration3To4)
        .addCallback(DefaultShiftsCallback)
        .build()

/**
 * Turnos de ejemplo. Los ids son **fijos** para que dos dispositivos que creen su base de forma
 * independiente no dupliquen los turnos al sincronizar; `updatedAt = 0` hace que cualquier edición real gane.
 */
private class Seed(val id: String, val name: String, val abbreviation: String, val color: Long, val start: Int, val end: Int)

private val DefaultSeeds = listOf(
    Seed("default-morning", "Mañana", "M", 0xFF2196F3L, 6 * 60, 14 * 60),
    Seed("default-afternoon", "Tarde", "T", 0xFFFF9800L, 14 * 60, 22 * 60),
    Seed("default-night", "Noche", "N", 0xFF3F51B5L, 22 * 60, 6 * 60),
)

internal fun defaultShiftEntities(): List<ShiftEntity> = DefaultSeeds.mapIndexed { index, s ->
    ShiftEntity(s.id, s.name, s.abbreviation, s.color, index, 0, null, true, s.start, s.end, 0)
}

private object DefaultShiftsCallback : RoomDatabase.Callback() {
    override fun onCreate(connection: SQLiteConnection) {
        DefaultSeeds.forEachIndexed { index, s ->
            connection.execSQL(
                "INSERT INTO shifts (id, name, abbreviation, colorArgb, sortOrder, updatedAt, deletedAt, dirty, startMinutes, endMinutes, breakMinutes) " +
                    "VALUES ('${s.id}', '${s.name}', '${s.abbreviation}', ${s.color}, $index, 0, NULL, 1, ${s.start}, ${s.end}, 0)"
            )
        }
    }
}
