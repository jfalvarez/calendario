package com.jfaf.calendario.presentation.calendar.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jfaf.calendario.domain.model.AppStyle
import com.jfaf.calendario.domain.model.CalendarDay
import com.jfaf.calendario.domain.model.DayNoteMarkers
import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.presentation.calendar.*
import com.jfaf.calendario.presentation.common.color
import com.jfaf.calendario.presentation.common.onShiftColor
import com.jfaf.calendario.presentation.theme.LocalAppStyle
import com.jfaf.calendario.resources.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RowScope.DayCell(
    day: CalendarDay,
    isToday: Boolean,
    isSelected: Boolean,
    markers: DayNoteMarkers?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val style = LocalAppStyle.current
    val colors = MaterialTheme.colorScheme
    val first = day.shifts.firstOrNull()
    // Pop: el día entero se pinta con el color de su primer turno.
    val fillShift = if (style == AppStyle.Pop) first else null
    val base = when {
        fillShift != null -> fillShift.color
        style == AppStyle.Sorbete && isToday -> colors.primary
        style == AppStyle.Neon -> Color.Transparent
        else -> colors.surfaceContainerLow
    }
    val background by animateColorAsState(if (isSelected) colors.primaryContainer else base)
    val scale by animateFloatAsState(if (isSelected) 0.93f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    val shape = MaterialTheme.shapes.small
    // Festivo (nacional o local): el número del día siempre en rojo, sea cual sea el fondo.
    // Festivo (nacional o local): el número del día siempre en rojo intenso, sea cual sea el fondo.
    val holidayRed = if (colors.surface.luminance() < 0.5f) Color(0xFFFF4D4D) else Color(0xFFD00000)
    val numberColor = when {
        day.holiday != null -> holidayRed
        isSelected -> colors.onPrimaryContainer
        fillShift != null -> fillShift.color.onShiftColor
        style == AppStyle.Sorbete && isToday -> colors.onPrimary
        else -> colors.onSurface
    }
    val outline = when {
        isSelected -> colors.primary
        style == AppStyle.Pop && isToday -> colors.onSurface
        else -> null
    }
    Column(
        Modifier
            .weight(1f)
            .fillMaxSize()
            .padding(2.dp)
            .scale(scale)
            .clip(shape)
            .background(background)
            .then(if (outline != null) Modifier.border(if (isSelected) 2.dp else 3.dp, outline, shape) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(3.dp)
            .alpha(if (day.inCurrentMonth) 1f else 0.35f),
        horizontalAlignment = if (style == AppStyle.Neon) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (style == AppStyle.Neon) {
            Box(
                Modifier.size(24.dp).background(if (isToday) colors.primary else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${day.date.day}", fontSize = 12.sp,
                    fontWeight = if (day.holiday != null || isToday) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isToday && day.holiday == null) colors.onPrimary else numberColor,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                day.shifts.forEach { Box(Modifier.size(8.dp).background(it.color, CircleShape)) }
            }
        } else {
            Text(
                "${day.date.day}", fontSize = 12.sp,
                fontWeight = if (isToday || fillShift != null || day.holiday != null) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = numberColor,
            )
            if (fillShift != null) {
                Text(fillShift.abbreviation, color = fillShift.color.onShiftColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                day.shifts.drop(1).forEach { ShiftTag(it, Modifier.fillMaxWidth()) }
            } else {
                day.shifts.forEach { ShiftTag(it, Modifier.fillMaxWidth()) }
            }
        }
if (markers != null) NoteMarker(markers)
    }
}

/** Cuadrícula de un mes: una fila por semana, cada día lo dibuja [dayCell]. */
@Composable
internal fun MonthWeeks(
    weeks: List<List<CalendarDay>>,
    modifier: Modifier,
    dayCell: @Composable RowScope.(CalendarDay) -> Unit,
) {
    Column(modifier) {
        weeks.forEach { week ->
            Row(Modifier.weight(1f).fillMaxWidth()) {
                week.forEach { day -> dayCell(day) }
            }
        }
    }
}

@Composable
internal fun ShiftTag(shift: Shift, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = 17.dp)
            .background(shift.color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(shift.abbreviation, color = shift.color.onShiftColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
