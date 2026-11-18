package com.jfaf.calendario.di

import com.jfaf.calendario.data.local.CalendarDatabase
import com.jfaf.calendario.data.remote.RemoteDataSource
import com.jfaf.calendario.data.remote.SupabaseAppConfigRepository
import com.jfaf.calendario.domain.repository.AppConfigRepository
import com.jfaf.calendario.data.remote.SupabaseAuthRepository
import com.jfaf.calendario.data.remote.SupabaseConfig
import com.jfaf.calendario.data.remote.SupabaseRemoteDataSource
import com.jfaf.calendario.data.remote.SupabaseSocialRepository
import com.jfaf.calendario.domain.repository.SocialRepository
import com.jfaf.calendario.presentation.social.SocialViewModel
import com.jfaf.calendario.presentation.social.SharedScheduleViewModel
import com.jfaf.calendario.data.sync.SyncEngine
import com.jfaf.calendario.domain.repository.AuthRepository
import com.jfaf.calendario.domain.repository.SyncRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.jfaf.calendario.data.prefs.DataStoreSettingsRepository
import com.jfaf.calendario.data.repository.RoomShiftAssignmentRepository
import com.jfaf.calendario.data.repository.RoomShiftRepository
import com.jfaf.calendario.data.repository.SystemTodayProvider
import com.jfaf.calendario.data.repository.UuidIdGenerator
import com.jfaf.calendario.domain.repository.IdGenerator
import com.jfaf.calendario.domain.repository.DayNoteRepository
import com.jfaf.calendario.data.repository.RoomDayNoteRepository
import com.jfaf.calendario.domain.usecase.DeleteDayNoteUseCase
import com.jfaf.calendario.domain.usecase.ObserveDayNotesUseCase
import com.jfaf.calendario.domain.usecase.ObserveDebtsUseCase
import com.jfaf.calendario.domain.usecase.SaveDayNoteUseCase
import com.jfaf.calendario.domain.usecase.SetNoteSettledUseCase
import com.jfaf.calendario.domain.repository.SettingsRepository
import com.jfaf.calendario.domain.repository.ReminderSettingsRepository
import com.jfaf.calendario.domain.usecase.ObserveReminderSettingsUseCase
import com.jfaf.calendario.domain.usecase.PlanNextReminderUseCase
import com.jfaf.calendario.domain.usecase.SetReminderEnabledUseCase
import com.jfaf.calendario.domain.usecase.SetReminderTimeUseCase
import org.koin.dsl.binds
import com.jfaf.calendario.domain.usecase.ObserveAppStyleUseCase
import com.jfaf.calendario.domain.usecase.SetAppStyleUseCase
import com.jfaf.calendario.domain.usecase.ObserveThemeModeUseCase
import com.jfaf.calendario.domain.usecase.SetThemeModeUseCase
import com.jfaf.calendario.presentation.settings.AccountViewModel
import com.jfaf.calendario.presentation.settings.SettingsViewModel
import com.jfaf.calendario.domain.usecase.AssignShiftToDaysUseCase
import com.jfaf.calendario.domain.usecase.ObserveAuthStateUseCase
import com.jfaf.calendario.domain.usecase.ObserveSyncStateUseCase
import com.jfaf.calendario.domain.usecase.RequestSyncUseCase
import com.jfaf.calendario.domain.usecase.SignInUseCase
import com.jfaf.calendario.domain.usecase.RequestPasswordResetUseCase
import com.jfaf.calendario.domain.usecase.CompletePasswordResetUseCase
import com.jfaf.calendario.domain.usecase.ChangePasswordUseCase
import com.jfaf.calendario.domain.usecase.SignOutUseCase
import com.jfaf.calendario.domain.usecase.SignUpUseCase
import com.jfaf.calendario.domain.usecase.ClearDaysUseCase
import com.jfaf.calendario.domain.usecase.ToggleLocalHolidaysUseCase
import com.jfaf.calendario.data.repository.RoomLocalHolidayRepository
import com.jfaf.calendario.domain.repository.LocalHolidayRepository
import com.jfaf.calendario.domain.usecase.CopyDaysUseCase
import com.jfaf.calendario.domain.usecase.CheckForUpdateUseCase
import com.jfaf.calendario.domain.usecase.DismissRecommendedUpdateUseCase
import com.jfaf.calendario.domain.usecase.ObserveDismissedUpdateUseCase
import com.jfaf.calendario.presentation.update.AppForegroundEvents
import com.jfaf.calendario.presentation.update.UpdateViewModel
import com.jfaf.calendario.domain.usecase.PasteDaysUseCase
import com.jfaf.calendario.domain.usecase.RepeatDaysUseCase
import com.jfaf.calendario.domain.usecase.DeleteShiftUseCase
import com.jfaf.calendario.domain.usecase.ReorderShiftsUseCase
import com.jfaf.calendario.domain.usecase.SaveShiftUseCase
import com.jfaf.calendario.presentation.shifts.ShiftsViewModel
import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import com.jfaf.calendario.domain.repository.ShiftRepository
import com.jfaf.calendario.domain.repository.TodayProvider
import com.jfaf.calendario.domain.usecase.ObserveMonthUseCase
import com.jfaf.calendario.domain.usecase.ObserveUpcomingDaysUseCase
import com.jfaf.calendario.domain.usecase.ObserveShiftsUseCase
import com.jfaf.calendario.domain.usecase.ObserveStatsUseCase
import com.jfaf.calendario.presentation.stats.StatsViewModel
import com.jfaf.calendario.presentation.debts.DebtsViewModel
import com.jfaf.calendario.domain.usecase.PaintDayUseCase
import com.jfaf.calendario.presentation.calendar.CalendarViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** `CalendarDatabase`, `DataStore<Preferences>` y `SupabaseConfig` y `AppPlatform` los aporta cada plataforma. */
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
    single<RemoteDataSource> { SupabaseRemoteDataSource(get()) }
    single<AppConfigRepository> { SupabaseAppConfigRepository(get(), get()) }
    single { SyncEngine(get(), get(), get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Default), errors = get()) }
    single<SyncRepository> { get<SyncEngine>() }
    single { DataStoreSettingsRepository(get()) } binds arrayOf(SettingsRepository::class, ReminderSettingsRepository::class)
    single { AppForegroundEvents() }
}

val domainModule = module {
    factory { ObserveShiftsUseCase(get()) }
    factory { ObserveStatsUseCase(get(), get()) }
    factory { ObserveMonthUseCase(get(), get(), get()) }
    factory { ObserveUpcomingDaysUseCase(get(), get(), get()) }
    factory { ObserveReminderSettingsUseCase(get()) }
    factory { SetReminderEnabledUseCase(get()) }
    factory { SetReminderTimeUseCase(get()) }
    factory { PlanNextReminderUseCase(get(), get(), get()) }
    factory { ObserveDayNotesUseCase(get()) }
    factory { ObserveDebtsUseCase(get()) }
    factory { SaveDayNoteUseCase(get(), get()) }
    factory { SetNoteSettledUseCase(get()) }
    factory { DeleteDayNoteUseCase(get()) }
    factory { PaintDayUseCase(get()) }
    factory { SignInUseCase(get()) }
    factory { SignUpUseCase(get()) }
    factory { SignOutUseCase(get()) }
    factory { RequestPasswordResetUseCase(get()) }
    factory { CompletePasswordResetUseCase(get()) }
    factory { ChangePasswordUseCase(get()) }
    factory { ObserveAuthStateUseCase(get()) }
    factory { ObserveSyncStateUseCase(get()) }
    factory { RequestSyncUseCase(get()) }
    factory { ObserveThemeModeUseCase(get()) }
    factory { SetThemeModeUseCase(get()) }
    factory { ObserveAppStyleUseCase(get()) }
    factory { SetAppStyleUseCase(get()) }
    factory { CopyDaysUseCase(get()) }
    factory { PasteDaysUseCase(get()) }
    factory { ClearDaysUseCase(get()) }
    factory { ToggleLocalHolidaysUseCase(get()) }
    factory { AssignShiftToDaysUseCase(get()) }
    factory { RepeatDaysUseCase(get()) }
    factory { SaveShiftUseCase(get(), get()) }
    factory { DeleteShiftUseCase(get()) }
    factory { ReorderShiftsUseCase(get()) }
    factory { CheckForUpdateUseCase(get(), get()) }
    factory { ObserveDismissedUpdateUseCase(get()) }
    factory { DismissRecommendedUpdateUseCase(get()) }
}

val presentationModule = module {
    viewModelOf(::CalendarViewModel)
    viewModelOf(::ShiftsViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AccountViewModel)
    viewModelOf(::SocialViewModel)
    viewModel { params -> SharedScheduleViewModel(params.get(), params.get(), get(), get(), get(), get()) }
    viewModelOf(::StatsViewModel)
    viewModelOf(::DebtsViewModel)
    viewModel { UpdateViewModel(get(), get(), get(), get<AppForegroundEvents>().events) }
}

val appModules = listOf(dataModule, domainModule, presentationModule)
