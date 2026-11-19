package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.CalendarMonth
import com.jfaf.calendario.domain.model.MyShiftOption
import com.jfaf.calendario.domain.model.PendingRequest
import com.jfaf.calendario.domain.repository.SocialError
import com.jfaf.calendario.domain.repository.toSocialError
import com.jfaf.calendario.fakes.FakeSocialRepository
import com.jfaf.calendario.fakes.InMemoryShiftAssignmentRepository
import com.jfaf.calendario.fakes.InMemoryShiftRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SocialUseCasesTest {
    private val social = FakeSocialRepository()

    @Test
    fun invalidAliasFailsWithoutCallingTheServer() = runTest {
        val result = SaveAliasUseCase(social)("a b")
        assertEquals(SocialError.InvalidAlias, result.exceptionOrNull()?.toSocialError())
        assertTrue(social.calls.none { it.startsWith("setAlias") })

        val link = RequestLinkUseCase(social)("")
        assertEquals(SocialError.InvalidAlias, link.exceptionOrNull()?.toSocialError())
        assertTrue(social.calls.none { it.startsWith("requestLink") })
    }

    @Test
    fun validAliasIsTrimmedBeforeSending() = runTest {
        val saved = SaveAliasUseCase(social)("  ana_77  ")
        assertEquals("ana_77", saved.getOrNull())
        assertTrue(social.calls.contains("setAlias:ana_77"))
    }

    @Test
    fun overviewIsEmptyWhenTheServerFails() = runTest {
        social.alias = "ana_77"
        social.pending = listOf(PendingRequest("l1", "pepe"))
        social.error = SocialError.Network
        val overview = LoadSocialOverviewUseCase(social, social)()
        // Con el error global, todas las partes quedan vacías; el alias también falla.
        assertNull(overview.alias)
        assertTrue(overview.pending.isEmpty())
        assertTrue(overview.blocked.isEmpty())
    }

    @Test
    fun overviewLoadsAllPartsWhenServerAnswers() = runTest {
        social.alias = "ana_77"
        social.pending = listOf(PendingRequest("l1", "pepe"))
        val overview = LoadSocialOverviewUseCase(social, social)()
        assertEquals("ana_77", overview.alias)
        assertEquals(listOf(PendingRequest("l1", "pepe")), overview.pending)
    }

    @Test
    fun swapOptionsCoverTheWindowWithNamesAndOrder() = runTest {
        val shifts = InMemoryShiftRepository()
        val assignments = InMemoryShiftAssignmentRepository()
        val from = LocalDate(2026, 11, 19)
        assignments.setShiftIds(LocalDate(2026, 11, 18), listOf("default-night"))
        assignments.setShiftIds(LocalDate(2026, 11, 20), listOf("default-night", "default-morning"))
        assignments.setShiftIds(LocalDate(2027, 1, 30), listOf("default-afternoon"))

        val options = BuildSwapOptionsUseCase(shifts, assignments)(from)

        assertEquals(
            listOf(
                MyShiftOption(LocalDate(2026, 11, 20), 0, "Noche"),
                MyShiftOption(LocalDate(2026, 11, 20), 1, "Mañana"),
            ),
            options,
        )
    }

    @Test
    fun monthOfSharedScheduleIsRequestedForTheRightOwner() = runTest {
        LoadSharedScheduleUseCase(social)("owner-1", CalendarMonth(2026, 12))
        assertTrue(social.calls.contains("schedule:owner-1:2026-12"))
    }
}
