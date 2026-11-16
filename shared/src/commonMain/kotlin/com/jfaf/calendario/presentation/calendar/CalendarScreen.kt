package com.jfaf.calendario.presentation.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jfaf.calendario.resources.Res
import com.jfaf.calendario.resources.app_name
import com.jfaf.calendario.resources.month_empty_hint
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Pantalla con estado (conecta el ViewModel) y contenido sin estado (testeable/previsualizable). */
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    CalendarContent(state)
}

@Composable
fun CalendarContent(state: CalendarUiState) {
    Column(Modifier.padding(16.dp)) {
        Text(stringResource(Res.string.app_name))
        Text(stringResource(Res.string.month_empty_hint))
        state.shifts.forEach { Text("${it.abbreviation} · ${it.name}") }
    }
}
