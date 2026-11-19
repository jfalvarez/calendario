package com.jfaf.calendario.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jfaf.calendario.MainActivity
import com.jfaf.calendario.R
import com.jfaf.calendario.domain.model.CalendarDay
import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.repository.TodayProvider
import com.jfaf.calendario.domain.usecase.ObserveUpcomingDaysUseCase
import kotlinx.coroutines.flow.first
import com.jfaf.calendario.AndroidServiceLocator
import androidx.compose.ui.unit.DpSize
import androidx.glance.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Column

/** Widget de pantalla de inicio: turno de hoy y de mañana. Los datos salen del mismo dominio que la app (Koin). */
class ShiftWidget : GlanceAppWidget() {
    // Dos tamaños de referencia: estrecho (sólo abreviatura) y ancho (con horario).
    override val sizeMode = SizeMode.Responsive(setOf(NARROW, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val days = AndroidServiceLocator.get<ObserveUpcomingDaysUseCase>()(AndroidServiceLocator.get<TodayProvider>().today(), days = 2).first()
        ShiftWidgetMidnight.schedule(context) // siguiente redibujado a medianoche, aunque no cambien datos
        provideContent { WidgetContent(context, days, withHours = LocalSize.current.width >= WIDE.width) }
    }

    private companion object {
        val NARROW = DpSize(110.dp, 40.dp)
        val WIDE = DpSize(180.dp, 40.dp)
    }
}

// Colores de la superficie del widget: claro/oscuro según el tema del sistema.
private val Surface = ColorProvider(day = Color(0xFFF4F8FC), night = Color(0xFF1C2229))
private val Muted = ColorProvider(day = Color(0xFF5B6672), night = Color(0xFFA8B3C0))

@Composable
private fun WidgetContent(context: Context, days: List<CalendarDay>, withHours: Boolean) {
    val labels = listOf(R.string.widget_today, R.string.widget_tomorrow)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Surface)
            .cornerRadius(20.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        days.forEachIndexed { index, day ->
            DayRow(
                context = context,
                label = context.getString(labels[index]),
                day = day,
                withHours = withHours,
                modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
            )
        }
    }
}

/** Una fila por día: etiqueta pequeña ("Hoy 17") y, debajo, los turnos. Cada día ocupa la mitad del widget. */
@Composable
private fun DayRow(context: Context, label: String, day: CalendarDay, withHours: Boolean, modifier: GlanceModifier) {
    Column(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label ${day.date.day}",
                style = TextStyle(color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
            )
            if (day.holiday != null) {
                Text(
                    text = context.getString(R.string.widget_holiday),
                    style = TextStyle(color = Muted, fontSize = 10.sp),
                    maxLines = 1,
                )
            }
        }
        if (day.shifts.isEmpty()) {
            Text(
                text = context.getString(R.string.widget_no_shift),
                style = TextStyle(color = Muted, fontSize = 12.sp),
                maxLines = 1,
            )
        } else {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                day.shifts.forEachIndexed { index, shift ->
                    if (index > 0) Spacer(GlanceModifier.width(4.dp))
                    ShiftChip(shift, withHours = withHours && day.shifts.size == 1, modifier = GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

@Composable
private fun ShiftChip(shift: Shift, withHours: Boolean, modifier: GlanceModifier) {
    val background = Color(shift.colorArgb)
    val onShift = ColorProvider(if (background.luminance() > 0.5f) Color.Black else Color.White)
    Row(
        modifier = modifier
            .cornerRadius(8.dp)
            .background(ColorProvider(background))
            .padding(horizontal = 6.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = shift.abbreviation, style = TextStyle(color = onShift, fontSize = 14.sp, fontWeight = FontWeight.Bold))
        val hours = shift.hoursLabel()
        if (withHours && hours != null) {
            Spacer(GlanceModifier.width(5.dp))
            Text(text = hours, style = TextStyle(color = onShift, fontSize = 10.sp), maxLines = 1)
        }
    }
}

private fun Shift.hoursLabel(): String? {
    val start = startMinutes ?: return null
    val end = endMinutes ?: return null
    return "${start.asClock()}–${end.asClock()}"
}

private fun Int.asClock(): String = "%02d:%02d".format(this / 60, this % 60)
