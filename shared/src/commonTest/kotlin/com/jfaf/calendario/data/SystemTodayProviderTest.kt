package com.jfaf.calendario.data

import com.jfaf.calendario.data.repository.SystemTodayProvider
import com.jfaf.calendario.fakes.FakeClock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class SystemTodayProviderTest {
    // 2026-11-19T23:30:00Z
    private val lateUtc = 1_795_131_000_000L

    @Test
    fun usesTheDateOfTheInjectedClockInTheGivenZone() {
        assertEquals(LocalDate(2026, 11, 19), SystemTodayProvider(FakeClock(lateUtc), UtcOffset.ZERO.asTimeZone()).today())
        // En UTC+1 ya es el día siguiente: la zona horaria cambia "hoy", no el instante.
        assertEquals(LocalDate(2026, 11, 20), SystemTodayProvider(FakeClock(lateUtc), UtcOffset(hours = 1).asTimeZone()).today())
    }

    @Test
    fun followsTheClockWhenItMoves() {
        val clock = FakeClock(lateUtc)
        val provider = SystemTodayProvider(clock, UtcOffset.ZERO.asTimeZone())
        clock.epochMillis += 60 * 60 * 1000L
        assertEquals(LocalDate(2026, 11, 20), provider.today())
    }
}
