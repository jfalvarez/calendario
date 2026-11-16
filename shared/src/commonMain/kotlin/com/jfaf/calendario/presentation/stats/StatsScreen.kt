package com.jfaf.calendario.presentation.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jfaf.calendario.domain.model.PeriodStats
import com.jfaf.calendario.domain.model.ShiftStats
import com.jfaf.calendario.presentation.common.color
import com.jfaf.calendario.presentation.common.onShiftColor
import com.jfaf.calendario.domain.model.StatsPeriod
import com.jfaf.calendario.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StatsScreen(onBack: () -> Unit, viewModel: StatsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    StatsContent(state, onBack, viewModel::onPeriodSelected, viewModel::onCustomRange)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsContent(
    state: StatsUiState,
    onBack: () -> Unit,
    onPeriodSelected: (StatsPeriod) -> Unit,
    onCustomRange: (LocalDate, LocalDate) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeriodChip(Res.string.period_this_month, StatsPeriod.ThisMonth, state.period, onPeriodSelected)
                PeriodChip(Res.string.period_last_month, StatsPeriod.LastMonth, state.period, onPeriodSelected)
                PeriodChip(Res.string.period_this_year, StatsPeriod.ThisYear, state.period, onPeriodSelected)
                PeriodChip(Res.string.period_last_30, StatsPeriod.Last30Days, state.period, onPeriodSelected)
                FilterChip(
                    selected = state.period == StatsPeriod.Custom,
                    onClick = { showPicker = true },
                    label = { Text(stringResource(Res.string.period_custom)) },
                )
            }
            Text("${formatDate(state.from)} – ${formatDate(state.to)}", style = MaterialTheme.typography.titleMedium)
            state.stats?.let { Summary(it) }
        }
    }

    if (showPicker) {
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                val start = pickerState.selectedStartDateMillis
                val end = pickerState.selectedEndDateMillis ?: start
                TextButton(
                    enabled = start != null,
                    onClick = {
                        showPicker = false
                        if (start != null && end != null) onCustomRange(start.toDate(), end.toDate())
                    },
                ) { Text(stringResource(Res.string.apply)) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(Res.string.cancel)) } },
        ) {
            DateRangePicker(pickerState, Modifier.heightIn(max = 520.dp))
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun Long.toDate(): LocalDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

@Composable
private fun PeriodChip(
    label: StringResource,
    period: StatsPeriod,
    selected: StatsPeriod,
    onSelect: (StatsPeriod) -> Unit,
) {
    FilterChip(selected = selected == period, onClick = { onSelect(period) }, label = { Text(stringResource(label)) })
}

@Composable
private fun Summary(stats: PeriodStats) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryCard(Res.string.stats_total_hours, duration(stats.totalMinutes), Modifier.weight(1f))
        SummaryCard(Res.string.stats_total_shifts, "${stats.totalShifts}", Modifier.weight(1f))
        SummaryCard(Res.string.stats_days_worked, "${stats.daysWorked}", Modifier.weight(1f))
    }
    if (stats.perShift.isEmpty()) {
        Text(stringResource(Res.string.stats_empty))
        return
    }
    Text(stringResource(Res.string.stats_by_shift), style = MaterialTheme.typography.titleMedium)
    stats.perShift.forEach { ShiftStatsRow(it) }
}

@Composable
private fun duration(minutes: Int): String =
    stringResource(Res.string.shift_duration, minutes / 60, (minutes % 60).toString().padStart(2, '0'))

@Composable
private fun SummaryCard(label: StringResource, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ShiftStatsRow(item: ShiftStats) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.width(40.dp).heightIn(min = 32.dp).background(Color(item.shift.colorArgb), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(item.shift.abbreviation, color = item.shift.color.onShiftColor, fontSize = 14.sp) }
        Column(Modifier.weight(1f)) {
            Text(item.shift.name, style = MaterialTheme.typography.titleSmall)
            Text(
                "${stringResource(Res.string.stats_count, item.count)} · ${duration(item.minutes)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
