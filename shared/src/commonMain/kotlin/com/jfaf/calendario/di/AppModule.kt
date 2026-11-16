package com.jfaf.calendario.di

import com.jfaf.calendario.data.repository.InMemoryShiftAssignmentRepository
import com.jfaf.calendario.data.repository.InMemoryShiftRepository
import com.jfaf.calendario.data.repository.SystemTodayProvider
import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import com.jfaf.calendario.domain.repository.ShiftRepository
import com.jfaf.calendario.domain.repository.TodayProvider
import com.jfaf.calendario.domain.usecase.ObserveMonthUseCase
import com.jfaf.calendario.domain.usecase.ObserveShiftsUseCase
import com.jfaf.calendario.domain.usecase.PaintDayUseCase
import com.jfaf.calendario.presentation.calendar.CalendarViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val dataModule = module {
    single<ShiftRepository> { InMemoryShiftRepository() }
    single<ShiftAssignmentRepository> { InMemoryShiftAssignmentRepository() }
    single<TodayProvider> { SystemTodayProvider() }
}

val domainModule = module {
    factory { ObserveShiftsUseCase(get()) }
    factory { ObserveMonthUseCase(get(), get()) }
    factory { PaintDayUseCase(get()) }
}

val presentationModule = module {
    viewModelOf(::CalendarViewModel)
}

val appModules = listOf(dataModule, domainModule, presentationModule)
