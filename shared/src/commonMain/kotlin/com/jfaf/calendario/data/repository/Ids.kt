package com.jfaf.calendario.data.repository

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Id único generado en cliente (válido sin conexión y sin colisiones entre dispositivos). */
@OptIn(ExperimentalUuidApi::class)
internal fun newId(): String = Uuid.random().toString()

/** Instante actual en ms UTC, para `updatedAt` y `deletedAt`. */
@OptIn(ExperimentalTime::class)
internal fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** Id determinista de una asignación: `fecha#posición` (ver `Migration3To4`). */
internal fun assignmentId(date: String, slot: Int): String = "$date#$slot"
