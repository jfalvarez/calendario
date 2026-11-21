package com.jfaf.calendario.fakes

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Reloj de pruebas: devuelve siempre [epochMillis] (ms UTC) y se puede mover con [epochMillis] = …. */
@OptIn(ExperimentalTime::class)
class FakeClock(var epochMillis: Long = 0L) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(epochMillis)
}
