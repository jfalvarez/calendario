package com.jfaf.calendario.domain

import com.jfaf.calendario.data.remote.socialErrorOf
import com.jfaf.calendario.domain.model.isValidAlias
import com.jfaf.calendario.domain.repository.SocialError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SocialValidationTest {

    @Test
    fun aliasAcceptsLettersDigitsAndUnderscoreWithinLength() {
        assertTrue(isValidAlias("maria_turnos"))
        assertTrue(isValidAlias("Juan"))
        assertTrue(isValidAlias("abc"))
        assertTrue(isValidAlias("a".repeat(20)))
    }

    @Test
    fun aliasRejectsBadLengthAndCharacters() {
        assertFalse(isValidAlias("ab"))
        assertFalse(isValidAlias("a".repeat(21)))
        assertFalse(isValidAlias("con espacio"))
        assertFalse(isValidAlias("ñandú"))
        assertFalse(isValidAlias("punto.no"))
        assertFalse(isValidAlias(""))
    }

    @Test
    fun aliasIgnoresSurroundingSpaces() {
        assertTrue(isValidAlias("  nurse  "))
    }

    @Test
    fun serverCodesMapToDomainErrors() {
        assertEquals(SocialError.AliasTaken, socialErrorOf(Exception("P0001: alias_taken")))
        assertEquals(SocialError.InvalidAlias, socialErrorOf(Exception("invalid_alias")))
        assertEquals(SocialError.AliasLocked, socialErrorOf(Exception("alias_locked")))
        assertEquals(SocialError.NotAuthenticated, socialErrorOf(Exception("not_authenticated")))
        assertEquals(SocialError.InvalidTransition, socialErrorOf(Exception("invalid_transition")))
        assertEquals(SocialError.Forbidden, socialErrorOf(Exception("forbidden")))
        assertEquals(SocialError.LinkNotFound, socialErrorOf(Exception("link_not_found")))
    }

    @Test
    fun unknownMessagesFallBackToUnknown() {
        assertEquals(SocialError.Unknown, socialErrorOf(Exception("algo raro")))
    }
}
