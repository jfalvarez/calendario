package com.jfaf.calendario.presentation.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jfaf.calendario.domain.model.DayNoteMarkers
import com.jfaf.calendario.resources.*

/**
 * Píldora de ancho completo, como la de un turno, para el día con notas y/o turnos debidos sin saldar. Lleva colores
 * propios (no los de un turno) para no confundirse con ellos: terciario si hay deuda pendiente, secundario si sólo hay nota.
 */
@Composable
internal fun NoteMarker(markers: DayNoteMarkers) {
    val colors = MaterialTheme.colorScheme
    val container = if (markers.hasOpenDebt) colors.tertiaryContainer else colors.secondaryContainer
    val content = if (markers.hasOpenDebt) colors.onTertiaryContainer else colors.onSecondaryContainer
    Row(
        Modifier.fillMaxWidth().heightIn(min = 17.dp).background(container, CircleShape),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (markers.hasNote) Icon(Icons.AutoMirrored.Filled.StickyNote2, null, Modifier.size(13.dp), tint = content)
        if (markers.hasOpenDebt) Icon(Icons.Filled.SwapHoriz, null, Modifier.size(15.dp), tint = content)
    }
}
