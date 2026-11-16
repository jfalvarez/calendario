package com.jfaf.calendario.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.List
import com.jfaf.calendario.resources.manage_shifts
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jfaf.calendario.domain.model.CalendarDay
import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.resources.Res
import com.jfaf.calendario.resources.month_names
import com.jfaf.calendario.resources.paint_mode_hint
import com.jfaf.calendario.resources.today
import com.jfaf.calendario.resources.weekday_short
import com.jfaf.calendario.resources.next_month
import com.jfaf.calendario.resources.previous_month
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Pantalla con estado (conecta el ViewModel) y contenido sin estado (testeable/previsualizable). */
@Composable
fun CalendarScreen(onOpenShifts: () -> Unit, viewModel: CalendarViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    CalendarContent(
        state = state,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onToday = viewModel::onToday,
        onShiftSelected = viewModel::onShiftSelected,
        onDayClick = viewModel::onDayClick,
        onOpenShifts = onOpenShifts,
    )
}

@Composable
fun CalendarContent(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onShiftSelected: (String) -> Unit,
    onDayClick: (kotlinx.datetime.LocalDate) -> Unit,
    onOpenShifts: () -> Unit,
) {
    val monthNames = stringArrayResource(Res.array.month_names)
    val weekdays = stringArrayResource(Res.array.weekday_short)
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(Res.string.previous_month))
            }
            Text(
                "${monthNames[state.month.monthNumber - 1]} ${state.month.year}",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(Res.string.next_month))
            }
            TextButton(onClick = onToday) { Text(stringResource(Res.string.today)) }
            IconButton(onClick = onOpenShifts) {
                Icon(Icons.AutoMirrored.Filled.List, stringResource(Res.string.manage_shifts))
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            weekdays.forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth()) {
            state.weeks.forEach { week ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    week.forEach { day ->
                        DayCell(day, isToday = day.date == state.today, onClick = { onDayClick(day.date) })
                    }
                }
            }
        }
        Text(
            stringResource(Res.string.paint_mode_hint),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.shifts.forEach { shift ->
                FilterChip(
                    selected = state.paintShiftId == shift.id,
                    onClick = { onShiftSelected(shift.id) },
                    label = { Text("${shift.abbreviation} · ${shift.name}") },
                )
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(day: CalendarDay, isToday: Boolean, onClick: () -> Unit) {
    val border = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(
        Modifier
            .weight(1f)
            .fillMaxSize()
            .padding(1.dp)
            .border(if (isToday) 2.dp else 1.dp, border, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(2.dp)
            .alpha(if (day.inCurrentMonth) 1f else 0.4f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text("${day.date.day}", fontSize = 12.sp)
        day.shifts.forEach { ShiftTag(it) }
    }
}

@Composable
private fun ShiftTag(shift: Shift) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 16.dp)
            .background(Color(shift.colorArgb), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(shift.abbreviation, color = Color.White, fontSize = 11.sp, maxLines = 1)
    }
}
