package com.jfaf.calendario.data.remote

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Fila de `public.shifts` en Supabase (ver `supabase/schema.sql`). */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class RemoteShift(
    @SerialName("owner_id") val ownerId: String,
    val id: String,
    val name: String,
    val abbreviation: String,
    @SerialName("color_argb") val colorArgb: Long,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("start_minutes") val startMinutes: Int? = null,
    @SerialName("end_minutes") val endMinutes: Int? = null,
    @SerialName("break_minutes") val breakMinutes: Int = 0,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null,
    /** Lo pone el servidor; nunca se envía al subir. */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
)

/** Fila de `public.shift_assignments` en Supabase. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class RemoteAssignment(
    @SerialName("owner_id") val ownerId: String,
    val id: String,
    @SerialName("assignment_date") val date: String,
    val slot: Int,
    @SerialName("shift_id") val shiftId: String,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
)

/** Acceso al servidor; lo implementa Supabase y los tests lo sustituyen por un falso. */
interface RemoteDataSource {
    suspend fun upsertShifts(rows: List<RemoteShift>)
    suspend fun upsertAssignments(rows: List<RemoteAssignment>)
    /** Filas con `server_updated_at >= since` (todas si es null), en orden ascendente. */
    suspend fun pullShifts(since: String?): List<RemoteShift>
    suspend fun pullAssignments(since: String?): List<RemoteAssignment>
}
