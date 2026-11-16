package com.jfaf.calendario.di

import com.jfaf.calendario.data.local.CalendarDatabase
import com.jfaf.calendario.data.remote.RemoteDataSource
import com.jfaf.calendario.data.remote.SupabaseAuthRepository
import com.jfaf.calendario.data.remote.SupabaseConfig
import com.jfaf.calendario.data.remote.SupabaseRemoteDataSource
import com.jfaf.calendario.data.sync.SyncEngine
import com.jfaf.calendario.domain.repository.AuthRepository
import com.jfaf.calendario.domain.repository.SyncRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.jfaf.calendario.data.prefs.DataStoreSettingsRepository
import com.jfaf.calendario.data.repository.RoomShiftAssignmentRepository
import com.jfaf.calendario.data.repository.RoomShiftRepository
import com.jfaf.calendario.data.repository.SystemTodayProvider
import com.jfaf.calendario.data.repository.UuidIdGenerator
import com.jfaf.calendario.domain.repository.IdGenerator
import com.jfaf.calendario.domain.repository.SettingsRepository
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
import com.jfaf.calendario.domain.usecase.SignOutUseCase
import com.jfaf.calendario.domain.usecase.SignUpUseCase
import com.jfaf.calendario.domain.usecase.ClearDaysUseCase
import com.jfaf.calendario.domain.usecase.CopyDaysUseCase
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
import com.jfaf.calendario.domain.usecase.ObserveShiftsUseCase
import com.jfaf.calendario.domain.usecase.ObserveStatsUseCase
import com.jfaf.calendario.presentation.stats.StatsViewModel
import com.jfaf.calendario.domain.usecase.PaintDayUseCase
import com.jfaf.calendario.presentation.calendar.CalendarViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** `CalendarDatabase`, `DataStore<Preferences>` y `SupabaseConfig` los aporta cada plataforma. */
val dataModule = module {
    single { get<CalendarDatabase>().shiftDao() }
    single { get<CalendarDatabase>().shiftAssignmentDao() }
    single<ShiftRepository> { RoomShiftRepository(get()) }
    single<ShiftAssignmentRepository> { RoomShiftAssignmentRepository(get()) }
    single<TodayProvider> { SystemTodayProvider() }
    single<IdGenerator> { UuidIdGenerator() }
    single<SupabaseClient> {
        val config = get<SupabaseConfig>()
        // Sin configurar se usa un destino inválido: la app funciona en local y la sincronización falla sin avisar.
        createSupabaseClient(
            supabaseUrl = if (config.isConfigured) config.url else "https://not-configured.invalid",
            supabaseKey = config.anonKey.ifBlank { "not-configured" },
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
    single<AuthRepository> { SupabaseAuthRepository(get()) }
    single<RemoteDataSource> { SupabaseRemoteDataSource(get()) }
    single { SyncEngine(get(), get(), get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
    single<SyncRepository> { get<SyncEngine>() }
    single<SettingsRepository> { DataStoreSettingsRepository(get()) }
}

val domainModule = module {
    factory { ObserveShiftsUseCase(get()) }
    factory { ObserveStatsUseCase(get(), get()) }
    factory { ObserveMonthUseCase(get(), get()) }
    factory { PaintDayUseCase(get()) }
    factory { SignInUseCase(get()) }
    factory { SignUpUseCase(get()) }
    factory { SignOutUseCase(get()) }
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
    factory { AssignShiftToDaysUseCase(get()) }
    factory { RepeatDaysUseCase(get()) }
    factory { SaveShiftUseCase(get(), get()) }
    factory { DeleteShiftUseCase(get()) }
    factory { ReorderShiftsUseCase(get()) }
}

val presentationModule = module {
    viewModelOf(::CalendarViewModel)
    viewModelOf(::ShiftsViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AccountViewModel)
    viewModelOf(::StatsViewModel)
}

val appModules = listOf(dataModule, domainModule, presentationModule)
