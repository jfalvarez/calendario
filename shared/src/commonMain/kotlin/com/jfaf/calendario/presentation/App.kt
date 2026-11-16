package com.jfaf.calendario.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jfaf.calendario.domain.model.ThemeMode
import com.jfaf.calendario.presentation.calendar.CalendarScreen
import com.jfaf.calendario.presentation.settings.SettingsScreen
import com.jfaf.calendario.presentation.settings.SettingsViewModel
import com.jfaf.calendario.presentation.shifts.ShiftsScreen
import org.koin.compose.viewmodel.koinViewModel

private object Routes {
    const val CALENDAR = "calendar"
    const val SHIFTS = "shifts"
    const val SETTINGS = "settings"
}

/**
 * Raíz común de la UI (Android y escritorio): tema y navegación.
 * `onDarkThemeChanged` avisa a la plataforma del tema efectivo (Android lo usa para el color de los iconos de las barras).
 */
@Composable
fun App(onDarkThemeChanged: (Boolean) -> Unit = {}) {
    val settings: SettingsViewModel = koinViewModel()
    val mode by settings.themeMode.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (mode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System, null -> systemDark
    }
    LaunchedEffect(darkTheme) { onDarkThemeChanged(darkTheme) }

    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (mode == null) return@Surface // ajustes aún sin leer: sólo el fondo, sin destello de tema
            val navController = rememberNavController()
            NavHost(navController, startDestination = Routes.CALENDAR) {
                composable(Routes.CALENDAR) {
                    CalendarScreen(
                        onOpenShifts = { navController.navigate(Routes.SHIFTS) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    )
                }
                composable(Routes.SHIFTS) { ShiftsScreen(onBack = { navController.popBackStack() }) }
                composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
            }
        }
    }
}
