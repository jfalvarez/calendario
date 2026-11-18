package com.jfaf.calendario.domain.repository

/** Token de avisos push del dispositivo. Solo Android lo tiene; en escritorio no se registra nada. */
interface PushTokenProvider {
    suspend fun currentToken(): String?
}
