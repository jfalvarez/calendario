package com.jfaf.calendario.di

import com.jfaf.calendario.data.local.CalendarDatabase
import com.jfaf.calendario.data.repository.RoomShiftAssignmentRepository
import com.jfaf.calendario.data.repository.RoomShiftRepository
import com.jfaf.calendario.data.repository.SystemTodayProvider
import com.jfaf.calendario.data.repository.UuidIdGenerator
import com.jfaf.calendario.domain.repository.IdGenerator
import com.jfaf.calendario.domain.usecase.DeleteShiftUseCase
import com.jfaf.calendario.domain.usecase.ReorderShiftsUseCase
import com.jfaf.calendario.domain.usecase.SaveShiftUseCase
import com.jfaf.calendario.presentation.shifts.ShiftsViewModel
import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import com.jfaf.calendario.domain.repository.ShiftRepository
import com.jfaf.calendario.domain.repository.TodayProvider
import com.jfaf.calendario.domain.usecase.ObserveMonthUseCase
import com.jfaf.calendario.domain.usecase.ObserveShiftsUseCase
import com.jfaf.calendario.domain.usecase.PaintDayUseCase
import com.jfaf.calendario.presentation.calendar.CalendarViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** `CalendarDatabase` lo aporta cada plataforma (`androidModule` / `desktopModule`). */
val dataModule = module {
    single { get<CalendarDatabase>().shiftDao() }
    single { get<CalendarDatabase>().shiftAssignmentDao() }
    single<ShiftRepository> { RoomShiftRepository(get()) }
    single<ShiftAssignmentRepository> { RoomShiftAssignmentRepository(get()) }
    single<TodayProvider> { SystemTodayProvider() }
    single<IdGenerator> { UuidIdGenerator() }
}

val domainModule = module {
    factory { ObserveShiftsUseCase(get()) }
    factory { ObserveMonthUseCase(get(), get()) }
    factory { PaintDayUseCase(get()) }
    factory { SaveShiftUseCase(get(), get()) }
    factory { DeleteShiftUseCase(get()) }
    factory { ReorderShiftsUseCase(get()) }
}

val presentationModule = module {
    viewModelOf(::CalendarViewModel)
    viewModelOf(::ShiftsViewModel)
}

val appModules = listOf(dataModule, domainModule, presentationModule)
