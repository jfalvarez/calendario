package com.jfaf.calendario.presentation

import com.jfaf.calendario.presentation.calendar.CalendarMode
import com.jfaf.calendario.presentation.calendar.DayTapAction
import com.jfaf.calendario.presentation.calendar.dayTapAction
import com.jfaf.calendario.presentation.calendar.longPressSelectsRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DayTapRoutingTest {
    @Test
    fun eachModeHasItsOwnTapAction() {
        assertEquals(DayTapAction.Paint, dayTapAction(CalendarMode.Paint))
        assertEquals(DayTapAction.Select, dayTapAction(CalendarMode.Edit))
        assertEquals(DayTapAction.OpenNotes, dayTapAction(CalendarMode.Notes))
    }

    @Test
    fun longPressOnlySelectsARangeInEditMode() {
        assertTrue(longPressSelectsRange(CalendarMode.Edit))
        assertFalse(longPressSelectsRange(CalendarMode.Paint))
        assertFalse(longPressSelectsRange(CalendarMode.Notes))
    }
}
