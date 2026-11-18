package com.jfaf.calendario.domain.model

import kotlinx.datetime.LocalDate

/** Longitud y formato del alias público (ver `supabase/social.sql`). */
const val ALIAS_MIN_LENGTH = 3
const val ALIAS_MAX_LENGTH = 20
private val ALIAS_REGEX = Regex("^[A-Za-z0-9_]{$ALIAS_MIN_LENGTH,$ALIAS_MAX_LENGTH}$")

/** Valida el alias antes de enviarlo: sólo letras, números y guion bajo. */
fun isValidAlias(alias: String): Boolean = ALIAS_REGEX.matches(alias.trim())

/** Resultado de pedir un vínculo por alias. `Sent` también cubre bloqueos y rechazos recientes (no se revelan). */
enum class LinkRequestResult { Sent, AliasNotFound, Self, TooManyAttempts, TooManyPending }

/** Solicitud de un compañero que espera mi respuesta. */
data class PendingRequest(val linkId: String, val requesterAlias: String)

enum class LinkDirection { Incoming, Outgoing }

enum class LinkStatus { Pending, Accepted, Rejected, Revoked }

/**
 * Vínculo visto desde mi cuenta. `Incoming`: otro ve mis turnos. `Outgoing`: yo veo los suyos.
 * `otherAlias` puede ser null si el otro usuario aún no tiene alias.
 */
data class LinkSummary(
    val linkId: String,
    val direction: LinkDirection,
    val otherId: String,
    val otherAlias: String?,
    val status: LinkStatus,
)

/** Turno asignado en el calendario de un compañero (solo lectura). `slot` 0 o 1: primer o segundo turno del día. */
data class SharedAssignment(
    val date: LocalDate,
    val slot: Int,
    val shiftName: String,
    val abbreviation: String,
    val colorArgb: Long,
    val startMinutes: Int?,
    val endMinutes: Int?,
    val breakMinutes: Int,
)
