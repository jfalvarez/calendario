package com.jfaf.calendario.data.repository

import com.jfaf.calendario.domain.repository.TodayProvider
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** "Hoy" a partir del reloj inyectado y la zona horaria del dispositivo (por defecto, la del sistema). */
@OptIn(ExperimentalTime::class)
class SystemTodayProvider(
    private val clock: Clock,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : TodayProvider {
    override fun today(): LocalDate = clock.now().toLocalDateTime(zone).date
}
