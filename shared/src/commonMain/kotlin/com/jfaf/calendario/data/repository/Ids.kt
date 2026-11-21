package com.jfaf.calendario.data.repository

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Id único generado en cliente (válido sin conexión y sin colisiones entre dispositivos). */
@OptIn(ExperimentalUuidApi::class)
internal fun newId(): String = Uuid.random().toString()

/** Id determinista de una asignación: `fecha#posición` (ver `Migration3To4`). */
internal fun assignmentId(date: String, slot: Int): String = "$date#$slot"
