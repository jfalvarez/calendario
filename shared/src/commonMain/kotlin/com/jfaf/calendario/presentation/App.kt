package com.jfaf.calendario.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import com.jfaf.calendario.presentation.theme.CalendarioTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.jfaf.calendario.domain.platform.BackupFileIo
import com.jfaf.calendario.presentation.backup.LocalBackupFileIo
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.jfaf.calendario.presentation.social.SharedScheduleScreen
import kotlinx.serialization.Serializable
import androidx.navigation.compose.rememberNavController
import com.jfaf.calendario.domain.model.AppStyle
import com.jfaf.calendario.domain.model.ThemeMode
import com.jfaf.calendario.presentation.calendar.CalendarScreen
import com.jfaf.calendario.presentation.debts.DebtsScreen
import com.jfaf.calendario.presentation.settings.SettingsScreen
import com.jfaf.calendario.presentation.settings.SettingsViewModel
import com.jfaf.calendario.presentation.shifts.ShiftsScreen
import com.jfaf.calendario.presentation.stats.StatsScreen
import com.jfaf.calendario.presentation.update.UpdateGate
import org.koin.compose.viewmodel.koinViewModel

private object Routes {
    const val CALENDAR = "calendar"
    const val SHIFTS = "shifts"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    const val DEBTS = "debts"
}

/** Calendario de solo lectura de un compañero (ruta con argumentos tipados). */
@Serializable
data class SharedRoute(val ownerId: String, val alias: String)

/**
 * Raíz común de la UI (Android y escritorio): tema y navegación.
 * `onDarkThemeChanged` avisa a la plataforma del tema efectivo (Android lo usa para el color de los iconos de las barras).
 */
@Composable
fun App(backupIo: BackupFileIo? = null, onDarkThemeChanged: (Boolean) -> Unit = {}) {
    CompositionLocalProvider(LocalBackupFileIo provides backupIo) {
        AppContent(onDarkThemeChanged)
    }
}

@Composable
private fun AppContent(onDarkThemeChanged: (Boolean) -> Unit) {
    val settings: SettingsViewModel = koinViewModel()
    val mode by settings.themeMode.collectAsState()
    val style by settings.appStyle.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (mode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System, null -> systemDark
    }
    LaunchedEffect(darkTheme) { onDarkThemeChanged(darkTheme) }

    CalendarioTheme(style ?: AppStyle.Pop, darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (mode == null || style == null) return@Surface // ajustes aún sin leer: sólo el fondo, sin destello de tema
            UpdateGate {
                val navController = rememberNavController()
                NavHost(navController, startDestination = Routes.CALENDAR) {
                    composable(Routes.CALENDAR) {
                        CalendarScreen(
                            onOpenShifts = { navController.navigate(Routes.SHIFTS) },
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                            onOpenStats = { navController.navigate(Routes.STATS) },
                            onOpenDebts = { navController.navigate(Routes.DEBTS) },
                        )
                    }
                    composable(Routes.SHIFTS) { ShiftsScreen(onBack = { navController.popBackStack() }) }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenShared = { ownerId, alias -> navController.navigate(SharedRoute(ownerId, alias)) },
                        )
                    }
                    composable<SharedRoute> { entry ->
                        val route = entry.toRoute<SharedRoute>()
                        SharedScheduleScreen(
                            ownerId = route.ownerId,
                            alias = route.alias,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(Routes.STATS) { StatsScreen(onBack = { navController.popBackStack() }) }
                    composable(Routes.DEBTS) { DebtsScreen(onBack = { navController.popBackStack() }) }
                }
            }
        }
    }
}
