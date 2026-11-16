package com.jfaf.calendario.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Campos de sincronización comunes (ver docs 07): `updatedAt` (ms UTC), `deletedAt` (borrado lógico)
 * y `dirty` (cambio local aún no enviado al servidor).
 */
@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey val id: String,
    val name: String,
    val abbreviation: String,
    val colorArgb: Long,
    val sortOrder: Int,
    val updatedAt: Long,
    val deletedAt: Long?,
    val dirty: Boolean,
    /** Hora de inicio en minutos desde medianoche (v2). */
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    @ColumnInfo(defaultValue = "0") val breakMinutes: Int = 0,
    /** Reservadas (v3), **sin uso**: el cálculo de sueldo está aplazado. Se mantienen para no romper la migración 2→3. */
    @ColumnInfo(defaultValue = "PerShift") val payMode: String = "PerShift",
    @ColumnInfo(defaultValue = "0") val payCents: Long = 0,
)

/** Un turno de un día en una posición (`slot` 0 o 1: máximo dos turnos por día). `date` en ISO (yyyy-MM-dd). */
@Entity(
    tableName = "shift_assignments",
    indices = [Index(value = ["date", "slot"], unique = true)],
)
data class ShiftAssignmentEntity(
    @PrimaryKey val id: String,
    val date: String,
    val slot: Int,
    val shiftId: String,
    val updatedAt: Long,
    val deletedAt: Long?,
    val dirty: Boolean,
)

/** Festivo local marcado por el usuario. La fecha (yyyy-MM-dd) es su identidad; sincroniza como el resto. */
@Entity(tableName = "local_holidays")
data class LocalHolidayEntity(
    @PrimaryKey val date: String,
    val updatedAt: Long,
    val deletedAt: Long?,
    val dirty: Boolean,
)
