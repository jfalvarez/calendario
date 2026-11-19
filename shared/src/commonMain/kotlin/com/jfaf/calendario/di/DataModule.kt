package com.jfaf.calendario.di

import com.jfaf.calendario.data.backup.RoomBackupStore
import com.jfaf.calendario.data.local.CalendarDatabase
import com.jfaf.calendario.data.prefs.DataStoreSettingsRepository
import com.jfaf.calendario.data.remote.RemoteDataSource
import com.jfaf.calendario.data.remote.SupabaseAppConfigRepository
import com.jfaf.calendario.data.remote.SupabaseAuthRepository
import com.jfaf.calendario.data.remote.SupabaseConfig
import com.jfaf.calendario.data.remote.SupabaseRemoteDataSource
import com.jfaf.calendario.data.remote.SupabaseSocialRepository
import com.jfaf.calendario.data.repository.RoomDayNoteRepository
import com.jfaf.calendario.data.repository.RoomLocalHolidayRepository
import com.jfaf.calendario.data.repository.RoomShiftAssignmentRepository
import com.jfaf.calendario.data.repository.RoomShiftRepository
import com.jfaf.calendario.data.repository.SystemTodayProvider
import com.jfaf.calendario.data.repository.UuidIdGenerator
import com.jfaf.calendario.data.sync.SyncEngine
import com.jfaf.calendario.domain.repository.AppConfigRepository
import com.jfaf.calendario.domain.repository.AuthRepository
import com.jfaf.calendario.domain.repository.BackupStore
import com.jfaf.calendario.domain.repository.DayNoteRepository
import com.jfaf.calendario.domain.repository.IdGenerator
import com.jfaf.calendario.domain.repository.LocalHolidayRepository
import com.jfaf.calendario.domain.repository.ReminderSettingsRepository
import com.jfaf.calendario.domain.repository.SettingsRepository
import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import com.jfaf.calendario.domain.repository.ShiftRepository
import com.jfaf.calendario.domain.repository.SocialRepository
import com.jfaf.calendario.domain.repository.SyncRepository
import com.jfaf.calendario.domain.repository.TodayProvider
import com.jfaf.calendario.presentation.update.AppForegroundEvents
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.dsl.binds
import org.koin.dsl.module

/** Persistencia, Supabase y sincronización. `CalendarDatabase`, `DataStore<Preferences>` y `SupabaseConfig` los aporta cada plataforma. */
val dataModule = module {
    single { get<CalendarDatabase>().shiftDao() }
    single { get<CalendarDatabase>().shiftAssignmentDao() }
    single<ShiftRepository> { RoomShiftRepository(get()) }
    single<ShiftAssignmentRepository> { RoomShiftAssignmentRepository(get()) }
    single { get<CalendarDatabase>().localHolidayDao() }
    single<LocalHolidayRepository> { RoomLocalHolidayRepository(get()) }
    single { get<CalendarDatabase>().dayNoteDao() }
    single<DayNoteRepository> { RoomDayNoteRepository(get()) }
    single<TodayProvider> { SystemTodayProvider() }
    single<IdGenerator> { UuidIdGenerator() }
    single<SupabaseClient> {
        val config = get<SupabaseConfig>()
        // Almacén de sesión propio de la plataforma (escritorio); si no hay, el de supabase-kt por defecto.
        val sessionStore = getOrNull<SessionManager>()
        // Sin configurar se usa un destino inválido: la app funciona en local y la sincronización falla sin avisar.
        createSupabaseClient(
            supabaseUrl = if (config.isConfigured) config.url else "https://not-configured.invalid",
            supabaseKey = config.anonKey.ifBlank { "not-configured" },
        ) {
            // Los nulos deben enviarse explícitamente: si `deleted_at: null` se omite, el upsert no limpia el borrado
            // lógico que ya hay en el servidor y la fila repintada reaparece borrada al descargar.
            defaultSerializer = KotlinXSerializer(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    explicitNulls = true
                },
            )
            install(Auth) { sessionStore?.let { sessionManager = it } }
            install(Postgrest)
        }
    }
    single<AuthRepository> { SupabaseAuthRepository(get()) }
    single<SocialRepository> { SupabaseSocialRepository(get()) }
    single<BackupStore> { RoomBackupStore(get()) }
    single<RemoteDataSource> { SupabaseRemoteDataSource(get()) }
    single<AppConfigRepository> { SupabaseAppConfigRepository(get(), get()) }
    single { SyncEngine(get(), get(), get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Default), errors = get()) }
    single<SyncRepository> { get<SyncEngine>() }
    single { DataStoreSettingsRepository(get()) } binds arrayOf(SettingsRepository::class, ReminderSettingsRepository::class)
    single { AppForegroundEvents() }
}
