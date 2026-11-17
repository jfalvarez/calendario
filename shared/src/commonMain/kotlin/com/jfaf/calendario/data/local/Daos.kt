package com.jfaf.calendario.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts WHERE deletedAt IS NULL ORDER BY sortOrder, name")
    fun observeAlive(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE deletedAt IS NULL ORDER BY sortOrder, name")
    suspend fun getAlive(): List<ShiftEntity>

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun get(id: String): ShiftEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM shifts")
    suspend fun nextSortOrder(): Int

    @Upsert
    suspend fun upsert(entity: ShiftEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ShiftEntity>)

    // --- sincronización ---
    @Query("SELECT * FROM shifts WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ShiftEntity>

    @Query("SELECT * FROM shifts WHERE dirty = 1")
    suspend fun getDirty(): List<ShiftEntity>

    /** Sólo limpia la marca si la fila no cambió desde que se envió (`updatedAt` igual). */
    @Query("UPDATE shifts SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markClean(id: String, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM shifts WHERE dirty = 1")
    fun observeDirtyCount(): Flow<Int>

    @Query("DELETE FROM shifts")
    suspend fun deleteAll()
}

@Dao
interface ShiftAssignmentDao {
    @Query("SELECT * FROM shift_assignments WHERE date BETWEEN :from AND :to AND deletedAt IS NULL ORDER BY date, slot")
    fun observeAlive(from: String, to: String): Flow<List<ShiftAssignmentEntity>>

    /** Filas del rango, incluidas las borradas lógicamente (para reutilizar su `id` y su posición). */
    @Query("SELECT * FROM shift_assignments WHERE date BETWEEN :from AND :to")
    suspend fun getInRange(from: String, to: String): List<ShiftAssignmentEntity>

    @Query("SELECT * FROM shift_assignments WHERE date = :date")
    suspend fun getForDate(date: String): List<ShiftAssignmentEntity>

    @Upsert
    suspend fun upsertAll(entities: List<ShiftAssignmentEntity>)

    // --- sincronización ---
    @Query("SELECT * FROM shift_assignments WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ShiftAssignmentEntity>

    @Query("SELECT * FROM shift_assignments WHERE dirty = 1")
    suspend fun getDirty(): List<ShiftAssignmentEntity>

    @Query("UPDATE shift_assignments SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markClean(id: String, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM shift_assignments WHERE dirty = 1")
    fun observeDirtyCount(): Flow<Int>

    @Query("DELETE FROM shift_assignments")
    suspend fun deleteAll()
}

@Dao
interface LocalHolidayDao {
    @Query("SELECT date FROM local_holidays WHERE date BETWEEN :from AND :to AND deletedAt IS NULL")
    fun observeAlive(from: String, to: String): Flow<List<String>>

    @Query("SELECT * FROM local_holidays WHERE date IN (:dates)")
    suspend fun getByDates(dates: List<String>): List<LocalHolidayEntity>

    @Upsert
    suspend fun upsertAll(entities: List<LocalHolidayEntity>)

    // --- sincronización ---
    @Query("SELECT * FROM local_holidays WHERE dirty = 1")
    suspend fun getDirty(): List<LocalHolidayEntity>

    @Query("UPDATE local_holidays SET dirty = 0 WHERE date = :date AND updatedAt = :updatedAt")
    suspend fun markClean(date: String, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM local_holidays WHERE dirty = 1")
    fun observeDirtyCount(): Flow<Int>

    @Query("DELETE FROM local_holidays")
    suspend fun deleteAll()
}

@Dao
interface DayNoteDao {
    @Query("SELECT * FROM day_notes WHERE date BETWEEN :from AND :to AND deletedAt IS NULL ORDER BY date, updatedAt")
    fun observeAlive(from: String, to: String): Flow<List<DayNoteEntity>>

    @Query("SELECT * FROM day_notes WHERE kind != 'Note' AND deletedAt IS NULL ORDER BY date DESC, updatedAt DESC")
    fun observeDebts(): Flow<List<DayNoteEntity>>

    @Query("SELECT * FROM day_notes WHERE id = :id")
    suspend fun get(id: String): DayNoteEntity?

    @Upsert
    suspend fun upsert(entity: DayNoteEntity)

    @Upsert
    suspend fun upsertAll(entities: List<DayNoteEntity>)

    // --- sincronización ---
    @Query("SELECT * FROM day_notes WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<DayNoteEntity>

    @Query("SELECT * FROM day_notes WHERE dirty = 1")
    suspend fun getDirty(): List<DayNoteEntity>

    @Query("UPDATE day_notes SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markClean(id: String, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM day_notes WHERE dirty = 1")
    fun observeDirtyCount(): Flow<Int>

    @Query("DELETE FROM day_notes")
    suspend fun deleteAll()
}
