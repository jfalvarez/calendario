package com.jfaf.calendario.di

import com.jfaf.calendario.data.repository.InMemoryShiftRepository
import com.jfaf.calendario.domain.repository.ShiftRepository
import com.jfaf.calendario.domain.usecase.ObserveShiftsUseCase
import com.jfaf.calendario.presentation.calendar.CalendarViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val dataModule = module {
    single<ShiftRepository> { InMemoryShiftRepository() }
}

val domainModule = module {
    factory { ObserveShiftsUseCase(get()) }
}

val presentationModule = module {
    viewModelOf(::CalendarViewModel)
}

val appModules = listOf(dataModule, domainModule, presentationModule)
