package com.jfaf.calendario.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jfaf.calendario.presentation.calendar.CalendarScreen

/** Raíz común de la UI (Android y escritorio). */
@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CalendarScreen()
        }
    }
}
