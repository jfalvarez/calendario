package com.jfaf.calendario.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts WHERE deletedAt IS NULL ORDER BY sortOrder, name")
    fun observeAlive(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun get(id: String): ShiftEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM shifts")
    suspend fun nextSortOrder(): Int

    @Upsert
    suspend fun upsert(entity: ShiftEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ShiftEntity>)

    @Query("SELECT * FROM shifts WHERE deletedAt IS NULL ORDER BY sortOrder, name")
    suspend fun getAlive(): List<ShiftEntity>
}

@Dao
interface ShiftAssignmentDao {
    @Query("SELECT * FROM shift_assignments WHERE date BETWEEN :from AND :to AND deletedAt IS NULL ORDER BY date, slot")
    fun observeAlive(from: String, to: String): Flow<List<ShiftAssignmentEntity>>

    /** Incluye las filas borradas lógicamente, para reutilizar su `id` y su posición. */
    @Query("SELECT * FROM shift_assignments WHERE date = :date")
    suspend fun getForDate(date: String): List<ShiftAssignmentEntity>

    @Upsert
    suspend fun upsertAll(entities: List<ShiftAssignmentEntity>)
}
