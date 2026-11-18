package com.jfaf.calendario.domain

import com.jfaf.calendario.domain.model.LinkDirection
import com.jfaf.calendario.domain.model.SwapProposal
import com.jfaf.calendario.domain.model.SwapStatus
import com.jfaf.calendario.domain.model.previewForMe
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SwapPreviewTest {
    private val proposal = SwapProposal(
        id = "s1",
        direction = LinkDirection.Outgoing,
        otherAlias = "Luis",
        proposerDate = LocalDate(2026, 11, 3),
        proposerSlot = 0,
        proposerShift = "Mañana",
        targetDate = LocalDate(2026, 11, 5),
        targetSlot = 1,
        targetShift = "Tarde",
        status = SwapStatus.Pending,
    )

    @Test
    fun proposerGivesItsDayAndReceivesTheTargetDay() {
        val (give, receive) = proposal.previewForMe()
        assertEquals(LocalDate(2026, 11, 3), give.date)
        assertEquals("Mañana", give.before)
        assertNull(give.after)
        assertEquals(LocalDate(2026, 11, 5), receive.date)
        assertNull(receive.before)
        assertEquals("Tarde", receive.after)
    }

    @Test
    fun targetSeesTheSameExchangeFromItsSide() {
        val (give, receive) = proposal.copy(direction = LinkDirection.Incoming).previewForMe()
        assertEquals(LocalDate(2026, 11, 5), give.date)
        assertEquals("Tarde", give.before)
        assertEquals(LocalDate(2026, 11, 3), receive.date)
        assertEquals("Mañana", receive.after)
    }
}
