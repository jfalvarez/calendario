package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.NextReminder
import com.jfaf.calendario.domain.model.ReminderSettings
import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.repository.ReminderSettingsRepository
import com.jfaf.calendario.domain.repository.ShiftAssignmentRepository
import com.jfaf.calendario.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class ObserveReminderSettingsUseCase(private val settings: ReminderSettingsRepository) {
    operator fun invoke(): Flow<ReminderSettings> = settings.observeReminderSettings()
}

class SetReminderEnabledUseCase(private val settings: ReminderSettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) = settings.setReminderEnabled(enabled)
}

class SetReminderTimeUseCase(private val settings: ReminderSettingsRepository) {
    suspend operator fun invoke(minutesOfDay: Int) {
        require(minutesOfDay in 0 until Shift.MINUTES_PER_DAY) { "Hora fuera de rango: $minutesOfDay" }
        settings.setReminderTime(minutesOfDay)
    }
}

/**
 * Calcula el siguiente aviso: el de la víspera (a la hora elegida) del primer día con turno de trabajo cuyo aviso
 * aún no ha pasado. Los turnos sin horario (libranza, vacaciones…) no generan aviso. Devuelve `null` si los avisos
 * están desactivados o no hay turnos de trabajo en los próximos [MAX_DAYS_AHEAD] días.
 */
class PlanNextReminderUseCase(
    private val settings: ReminderSettingsRepository,
    private val shifts: ShiftRepository,
    private val assignments: ShiftAssignmentRepository,
) {
    suspend operator fun invoke(now: LocalDateTime): NextReminder? {
        val config = settings.observeReminderSettings().first()
        if (!config.enabled) return null
        val workIds = shifts.observeShifts().first().filter { it.startMinutes != null }.map { it.id }.toSet()
        // El aviso del turno de hoy ya pasó (era ayer): el primer candidato es mañana, con aviso hoy.
        val first = now.date.plus(DatePeriod(days = 1))
        val last = first.plus(DatePeriod(days = MAX_DAYS_AHEAD))
        val byDate = assignments.getAssignments(first, last)
        val time = LocalTime(config.minutesOfDay / 60, config.minutesOfDay % 60)
        var date: LocalDate = first
        while (date <= last) {
            val trigger = LocalDateTime(date.minus(DatePeriod(days = 1)), time)
            if (trigger > now && byDate[date].orEmpty().any { it in workIds }) return NextReminder(date, trigger)
            date = date.plus(DatePeriod(days = 1))
        }
        return null
    }

    companion object {
        const val MAX_DAYS_AHEAD = 60
    }
}
