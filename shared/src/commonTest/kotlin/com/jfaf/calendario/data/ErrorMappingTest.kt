package com.jfaf.calendario.data

import com.jfaf.calendario.data.remote.looksOffline
import com.jfaf.calendario.data.remote.mapRestError
import com.jfaf.calendario.domain.model.AuthError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ErrorMappingTest {
    @Test
    fun mapsSupabaseAuthErrors() {
        assertEquals(AuthError.InvalidCredentials, mapRestError("invalid_credentials", "Invalid login credentials"))
        assertEquals(AuthError.EmailAlreadyUsed, mapRestError("user_already_exists", "User already registered"))
        assertEquals(AuthError.WeakPassword, mapRestError("weak_password", "Password should be at least 6 characters"))
        assertEquals(AuthError.InvalidEmail, mapRestError("email_address_invalid", "Email address is invalid"))
        assertEquals(AuthError.InvalidCode, mapRestError("otp_expired", "Token has expired or is invalid"))
        assertEquals(AuthError.TooManyRequests, mapRestError("over_email_send_rate_limit", "Email rate limit exceeded"))
        assertEquals(AuthError.Unknown, mapRestError("otro", null))
    }

    private class UnknownHostException : Exception()

    @Test
    fun detectsConnectivityFailuresThroughTheCauseChain() {
        assertTrue(looksOffline(UnknownHostException()))
        assertTrue(looksOffline(RuntimeException("envoltorio", UnknownHostException())))
        assertFalse(looksOffline(IllegalStateException("otra cosa")))
    }
}
