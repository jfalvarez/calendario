package com.jfaf.calendario.presentation.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jfaf.calendario.domain.model.SharedAssignment
import com.jfaf.calendario.presentation.social.ProposeSwapDialog
import com.jfaf.calendario.resources.*
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf
import androidx.compose.ui.text.style.TextAlign

/** Calendario de solo lectura de un compañero que me ha aceptado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedScheduleScreen(
    ownerId: String,
    alias: String,
    onBack: () -> Unit,
    viewModel: SharedScheduleViewModel = koinViewModel(parameters = { parametersOf(ownerId, alias) }),
) {
    val state by viewModel.uiState.collectAsState()
    val monthNames = stringArrayResource(Res.array.month_names)
    val weekdays = stringArrayResource(Res.array.weekday_short)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.shared_title, state.alias)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            state.proposal?.let { draft ->
                ProposeSwapDialog(
                    draft = draft,
                    onSelect = viewModel::selectOption,
                    onSend = viewModel::sendProposal,
                    onDismiss = viewModel::closeProposal,
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::onPreviousMonth) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.previous_month))
                }
                Text(
                    "${monthNames[state.month.monthNumber - 1]} ${state.month.year}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = viewModel::onNextMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(Res.string.next_month))
                }
            }

            when {
                state.loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Text(
                    stringResource(Res.string.shared_error),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
                state.assignments.isEmpty() -> Text(
                    stringResource(Res.string.shared_empty),
                    modifier = Modifier.padding(top = 16.dp),
                )
                else -> {
                    Text(
                        stringResource(Res.string.swap_hint),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                    val byDate = state.assignments.groupBy { it.date }.toSortedMap()
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        byDate.forEach { (date, dayAssignments) ->
                            item(key = date.toString()) {
                                DayBlock(date, dayAssignments, weekdays[date.dayOfWeek.ordinal], viewModel::openProposal)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayBlock(date: LocalDate, assignments: List<SharedAssignment>, weekday: String, onPropose: (SharedAssignment) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$weekday ${date.day}", style = MaterialTheme.typography.titleSmall)
        assignments.sortedBy { it.slot }.forEach { ShiftLine(it, onPropose) }
        HorizontalDivider(Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ShiftLine(a: SharedAssignment, onPropose: (SharedAssignment) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onPropose(a) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(12.dp).background(Color(a.colorArgb.toInt()), CircleShape))
        Text(a.shiftName + (a.timeRange()?.let { " · $it" } ?: ""))
    }
}

/** "07:00–15:00", o null si el turno no tiene horario. */
internal fun SharedAssignment.timeRange(): String? {
    val start = startMinutes ?: return null
    val end = endMinutes ?: return null
    return "${hhmm(start)}–${hhmm(end)}"
}

private fun hhmm(minutes: Int): String = "${(minutes / 60).toString().padStart(2, '0')}:${(minutes % 60).toString().padStart(2, '0')}"
