package com.jfaf.calendario.presentation.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.jfaf.calendario.domain.model.Shift

/** Color de fondo con el que se pinta un turno. */
val Shift.color: Color get() = Color(colorArgb)

/** Texto legible sobre un fondo de turno: negro sobre colores claros (amarillo…) y blanco sobre oscuros. */
val Color.onShiftColor: Color get() = if (luminance() > 0.5f) Color.Black else Color.White
