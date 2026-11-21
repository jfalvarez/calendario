package com.jfaf.calendario.data

import com.jfaf.calendario.data.remote.looksExpiredSession
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExpiredSessionTest {
    @Test
    fun recognizesTheServerJwtExpiredError() {
        assertTrue(looksExpiredSession(Exception("JWT expired\nCode: PGRST303\nHint: null")))
        assertTrue(looksExpiredSession(RuntimeException("wrapper", Exception("JWT expired"))))
    }

    @Test
    fun otherErrorsAreNotAnExpiredSession() {
        assertFalse(looksExpiredSession(Exception("timeout")))
        assertFalse(looksExpiredSession(Exception("permission denied")))
    }
}
