package com.jfaf.calendario.presentation.calendar

/** Qué hace un toque sobre un día, según el modo de la pantalla. */
enum class DayTapAction { Paint, Select, OpenNotes }

/** Decide la acción de un toque en un día: cada modo tiene la suya. */
fun dayTapAction(mode: CalendarMode): DayTapAction = when (mode) {
    CalendarMode.Paint -> DayTapAction.Paint
    CalendarMode.Edit -> DayTapAction.Select
    CalendarMode.Notes -> DayTapAction.OpenNotes
}

/** La pulsación larga solo selecciona un rango en modo Editar. */
fun longPressSelectsRange(mode: CalendarMode): Boolean = mode == CalendarMode.Edit
