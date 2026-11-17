package com.jfaf.calendario.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class SupabaseRemoteDataSource(private val client: SupabaseClient) : RemoteDataSource {

    override suspend fun upsertShifts(rows: List<RemoteShift>) {
        rows.chunked(BATCH).forEach { client.from("shifts").upsert(it) { onConflict = "owner_id,id" } }
    }

    override suspend fun upsertAssignments(rows: List<RemoteAssignment>) {
        rows.chunked(BATCH).forEach { client.from("shift_assignments").upsert(it) { onConflict = "owner_id,id" } }
    }

    override suspend fun upsertHolidays(rows: List<RemoteLocalHoliday>) {
        rows.chunked(BATCH).forEach { client.from("local_holidays").upsert(it) { onConflict = "owner_id,holiday_date" } }
    }

    override suspend fun pullHolidays(since: String?): List<RemoteLocalHoliday> = pullAll("local_holidays", since, "holiday_date")

    override suspend fun upsertDayNotes(rows: List<RemoteDayNote>) {
        rows.chunked(BATCH).forEach { client.from("day_notes").upsert(it) { onConflict = "owner_id,id" } }
    }

    override suspend fun pullDayNotes(since: String?): List<RemoteDayNote> = pullAll("day_notes", since)

    override suspend fun pullShifts(since: String?): List<RemoteShift> = pullAll("shifts", since)

    override suspend fun pullAssignments(since: String?): List<RemoteAssignment> = pullAll("shift_assignments", since)

    /** Descarga paginada, ordenada por `server_updated_at` y `id` para que la paginación sea estable. */
    private suspend inline fun <reified T : Any> pullAll(table: String, since: String?, idColumn: String = "id"): List<T> {
        val result = mutableListOf<T>()
        var offset = 0L
        while (true) {
            val page = client.from(table).select {
                if (since != null) filter { gte("server_updated_at", since) }
                order("server_updated_at", Order.ASCENDING)
                order(idColumn, Order.ASCENDING)
                range(offset, offset + PAGE - 1)
            }.decodeList<T>()
            result += page
            if (page.size < PAGE) return result
            offset += PAGE
        }
    }

    private companion object {
        const val BATCH = 500
        const val PAGE = 1000L
    }
}
