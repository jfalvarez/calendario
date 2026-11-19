package com.jfaf.calendario.di

import com.jfaf.calendario.presentation.backup.BackupViewModel
import com.jfaf.calendario.presentation.calendar.CalendarViewModel
import com.jfaf.calendario.presentation.debts.DebtsViewModel
import com.jfaf.calendario.presentation.settings.AccountViewModel
import com.jfaf.calendario.presentation.settings.SettingsViewModel
import com.jfaf.calendario.presentation.shifts.ShiftsViewModel
import com.jfaf.calendario.presentation.social.SharedScheduleViewModel
import com.jfaf.calendario.presentation.social.SocialViewModel
import com.jfaf.calendario.presentation.stats.StatsViewModel
import com.jfaf.calendario.presentation.update.AppForegroundEvents
import com.jfaf.calendario.presentation.update.UpdateViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** ViewModels de todas las pantallas. */
val presentationModule = module {
    viewModelOf(::CalendarViewModel)
    viewModelOf(::ShiftsViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AccountViewModel)
    viewModelOf(::SocialViewModel)
    viewModelOf(::BackupViewModel)
    viewModel { params -> SharedScheduleViewModel(params.get(), params.get(), get(), get(), get(), get()) }
    viewModelOf(::StatsViewModel)
    viewModelOf(::DebtsViewModel)
    viewModel { UpdateViewModel(get(), get(), get(), get<AppForegroundEvents>().events) }
}
