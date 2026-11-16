package com.jfaf.calendario.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jfaf.calendario.presentation.calendar.CalendarScreen
import com.jfaf.calendario.presentation.shifts.ShiftsScreen

private object Routes {
    const val CALENDAR = "calendar"
    const val SHIFTS = "shifts"
}

/** Raíz común de la UI (Android y escritorio): tema y navegación. */
@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            NavHost(navController, startDestination = Routes.CALENDAR) {
                composable(Routes.CALENDAR) {
                    CalendarScreen(onOpenShifts = { navController.navigate(Routes.SHIFTS) })
                }
                composable(Routes.SHIFTS) {
                    ShiftsScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
