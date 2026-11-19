package com.jfaf.calendario.di

import com.jfaf.calendario.domain.usecase.AssignShiftToDaysUseCase
import com.jfaf.calendario.domain.usecase.ChangePasswordUseCase
import com.jfaf.calendario.domain.usecase.CheckForUpdateUseCase
import com.jfaf.calendario.domain.usecase.ClearDaysUseCase
import com.jfaf.calendario.domain.usecase.CompletePasswordResetUseCase
import com.jfaf.calendario.domain.usecase.CopyDaysUseCase
import com.jfaf.calendario.domain.usecase.DeleteDayNoteUseCase
import com.jfaf.calendario.domain.usecase.DeleteShiftUseCase
import com.jfaf.calendario.domain.usecase.DismissRecommendedUpdateUseCase
import com.jfaf.calendario.domain.usecase.ExportBackupUseCase
import com.jfaf.calendario.domain.usecase.ImportBackupUseCase
import com.jfaf.calendario.domain.usecase.ObserveAppStyleUseCase
import com.jfaf.calendario.domain.usecase.ObserveAuthStateUseCase
import com.jfaf.calendario.domain.usecase.ObserveDayNotesUseCase
import com.jfaf.calendario.domain.usecase.ObserveDebtsUseCase
import com.jfaf.calendario.domain.usecase.ObserveDismissedUpdateUseCase
import com.jfaf.calendario.domain.usecase.ObserveStatsUseCase
import com.jfaf.calendario.domain.usecase.ObserveMonthUseCase
import com.jfaf.calendario.domain.usecase.ObserveReminderSettingsUseCase
import com.jfaf.calendario.domain.usecase.ObserveShiftsUseCase
import com.jfaf.calendario.domain.usecase.ObserveSyncStateUseCase
import com.jfaf.calendario.domain.usecase.ObserveThemeModeUseCase
import com.jfaf.calendario.domain.usecase.ObserveUpcomingDaysUseCase
import com.jfaf.calendario.domain.usecase.PaintDayUseCase
import com.jfaf.calendario.domain.usecase.PasteDaysUseCase
import com.jfaf.calendario.domain.usecase.PlanNextReminderUseCase
import com.jfaf.calendario.domain.usecase.PushRegistration
import com.jfaf.calendario.domain.usecase.ReorderShiftsUseCase
import com.jfaf.calendario.domain.usecase.RepeatDaysUseCase
import com.jfaf.calendario.domain.usecase.RequestPasswordResetUseCase
import com.jfaf.calendario.domain.usecase.RequestSyncUseCase
import com.jfaf.calendario.domain.usecase.SaveDayNoteUseCase
import com.jfaf.calendario.domain.usecase.SaveShiftUseCase
import com.jfaf.calendario.domain.usecase.SetAppStyleUseCase
import com.jfaf.calendario.domain.usecase.SetNoteSettledUseCase
import com.jfaf.calendario.domain.usecase.SetReminderEnabledUseCase
import com.jfaf.calendario.domain.usecase.SetReminderTimeUseCase
import com.jfaf.calendario.domain.usecase.SetThemeModeUseCase
import com.jfaf.calendario.domain.usecase.SignInUseCase
import com.jfaf.calendario.domain.usecase.SignOutUseCase
import com.jfaf.calendario.domain.usecase.SignUpUseCase
import com.jfaf.calendario.domain.usecase.ToggleLocalHolidaysUseCase
import org.koin.dsl.module

/** Casos de uso: una factoría por intención (ver `docs/revision-arquitectura.md`, Fase 6). */
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
    factory { PushRegistration(get(), getOrNull()) }
    factory { ExportBackupUseCase(get()) }
    factory { ImportBackupUseCase(get()) }
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
