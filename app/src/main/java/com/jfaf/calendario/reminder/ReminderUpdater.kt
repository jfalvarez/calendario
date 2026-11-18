package com.jfaf.calendario.reminder

import com.jfaf.calendario.domain.repository.ReminderSettingsRepository
import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import com.jfaf.calendario.domain.repository.ShiftRepository
import com.jfaf.calendario.domain.repository.TodayProvider
import com.jfaf.calendario.domain.usecase.PlanNextReminderUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

/**
 * Reprograma el aviso cuando cambian los ajustes, los turnos o las asignaciones (pintar, sincronizar…).
 * [start] reinicia la observación: la ventana de fechas parte de hoy, así que se llama al arrancar y al volver a primer plano.
 */
class ReminderUpdater(
    private val settings: ReminderSettingsRepository,
    private val shifts: ShiftRepository,
    private val assignments: ShiftAssignmentRepository,
    private val today: TodayProvider,
    private val scheduler: ReminderScheduler,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    fun start() {
        job?.cancel()
        val from = today.today()
        val to = from.plus(DatePeriod(days = PlanNextReminderUseCase.MAX_DAYS_AHEAD + 1))
        job = scope.launch {
            combine(
                settings.observeReminderSettings(),
                shifts.observeShifts(),
                assignments.observeAssignments(from, to),
            ) { _, _, _ -> }.collect { scheduler.reschedule() }
        }
    }
}
