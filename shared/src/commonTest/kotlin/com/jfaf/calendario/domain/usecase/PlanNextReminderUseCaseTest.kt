package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.ReminderSettings
import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.repository.ReminderSettingsRepository
import com.jfaf.calendario.fakes.InMemoryShiftAssignmentRepository
import com.jfaf.calendario.fakes.InMemoryShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlanNextReminderUseCaseTest {
    private class FakeReminderSettings(initial: ReminderSettings) : ReminderSettingsRepository {
        val state = MutableStateFlow(initial)
        override fun observeReminderSettings(): Flow<ReminderSettings> = state
        override suspend fun setReminderEnabled(enabled: Boolean) { state.value = state.value.copy(enabled = enabled) }
        override suspend fun setReminderTime(minutesOfDay: Int) { state.value = state.value.copy(minutesOfDay = minutesOfDay) }
    }

    // Los turnos por defecto del fake (M, T, N) no tienen horario; se añaden dos con horario y uno de libranza.
    private val shifts = InMemoryShiftRepository()
    private val assignments = InMemoryShiftAssignmentRepository()
    private val settings = FakeReminderSettings(ReminderSettings(enabled = true, minutesOfDay = 21 * 60))
    private val plan = PlanNextReminderUseCase(settings, shifts, assignments)
    private val now = LocalDateTime(2025, 11, 12, 10, 0)

    private suspend fun givenShifts() {
        shifts.upsert(Shift("work", "Mañana", "M", 0xFF2196F3, startMinutes = 7 * 60, endMinutes = 15 * 60))
        shifts.upsert(Shift("off", "Libre", "L", 0xFF9E9E9E))
    }

    @Test
    fun remindsTheEveningBeforeTheNextWorkShift() = runTest {
        givenShifts()
        assignments.setShiftIds(LocalDate(2025, 11, 14), listOf("work"))

        val next = plan(now)!!

        assertEquals(LocalDate(2025, 11, 14), next.shiftDate)
        assertEquals(LocalDateTime(2025, 11, 13, 21, 0), next.triggerAt)
    }

    @Test
    fun usesTheTimeChosenByTheUser() = runTest {
        givenShifts()
        assignments.setShiftIds(LocalDate(2025, 11, 14), listOf("work"))
        settings.setReminderTime(18 * 60 + 30)

        assertEquals(LocalDateTime(2025, 11, 13, 18, 30), plan(now)!!.triggerAt)
    }

    @Test
    fun tomorrowsShiftIsRemindedTodayIfTheTimeHasNotPassed() = runTest {
        givenShifts()
        assignments.setShiftIds(LocalDate(2025, 11, 13), listOf("work"))

        assertEquals(LocalDateTime(2025, 11, 12, 21, 0), plan(now)!!.triggerAt)
    }

    @Test
    fun skipsTomorrowIfTodaysReminderTimeAlreadyPassed() = runTest {
        givenShifts()
        assignments.setShiftIds(LocalDate(2025, 11, 13), listOf("work"))
        assignments.setShiftIds(LocalDate(2025, 11, 15), listOf("work"))

        val next = plan(LocalDateTime(2025, 11, 12, 22, 0))!!

        assertEquals(LocalDate(2025, 11, 15), next.shiftDate)
    }

    @Test
    fun daysOffAndShiftsWithoutScheduleDoNotRemind() = runTest {
        givenShifts()
        assignments.setShiftIds(LocalDate(2025, 11, 14), listOf("off"))
        assignments.setShiftIds(LocalDate(2025, 11, 15), listOf("default-morning"))

        assertNull(plan(now))
    }

    @Test
    fun nothingWhenDisabled() = runTest {
        givenShifts()
        assignments.setShiftIds(LocalDate(2025, 11, 14), listOf("work"))
        settings.setReminderEnabled(false)

        assertNull(plan(now))
    }

    @Test
    fun anyWorkShiftOfADayIsEnough() = runTest {
        givenShifts()
        assignments.setShiftIds(LocalDate(2025, 11, 14), listOf("off", "work"))

        assertEquals(LocalDate(2025, 11, 14), plan(now)!!.shiftDate)
    }
}
