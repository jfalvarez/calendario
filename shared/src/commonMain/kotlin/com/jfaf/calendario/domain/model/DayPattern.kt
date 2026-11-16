package com.jfaf.calendario.domain.model

/**
 * Bloque de días copiado: un elemento por día desde el primero al último seleccionado.
 * `null` = ese día no estaba seleccionado (se salta al pegar o repetir); lista vacía = día sin turnos.
 */
data class DayPattern(val days: List<List<String>?>)
