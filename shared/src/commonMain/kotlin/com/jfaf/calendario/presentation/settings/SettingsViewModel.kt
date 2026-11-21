package com.jfaf.calendario.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.model.AppStyle
import com.jfaf.calendario.domain.model.ReminderSettings
import com.jfaf.calendario.domain.model.ThemeMode
import com.jfaf.calendario.domain.platform.ReminderSystem
import com.jfaf.calendario.domain.usecase.ObserveAppStyleUseCase
import com.jfaf.calendario.domain.usecase.ObserveReminderSettingsUseCase
import com.jfaf.calendario.domain.usecase.ObserveThemeModeUseCase
import com.jfaf.calendario.domain.usecase.SetAppStyleUseCase
import com.jfaf.calendario.domain.usecase.SetReminderEnabledUseCase
import com.jfaf.calendario.domain.usecase.SetReminderTimeUseCase
import com.jfaf.calendario.domain.usecase.SetThemeModeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de la sección «Recordatorios» de Ajustes. */
data class ReminderUiState(
    val supported: Boolean = false,
    val enabled: Boolean = false,
    val minutesOfDay: Int = ReminderSettings.DEFAULT_MINUTES_OF_DAY,
    /** El usuario rechazó el permiso de notificaciones al activar los avisos. */
    val permissionDenied: Boolean = false,
)

class SettingsViewModel(
    observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    observeAppStyle: ObserveAppStyleUseCase,
    private val setAppStyle: SetAppStyleUseCase,
    observeReminder: ObserveReminderSettingsUseCase,
    private val setReminderEnabled: SetReminderEnabledUseCase,
    private val setReminderTime: SetReminderTimeUseCase,
    private val reminderSystem: ReminderSystem,
) : ViewModel() {
    /** `null` mientras se leen los ajustes (evita un destello con el tema equivocado al arrancar). */
    val themeMode: StateFlow<ThemeMode?> =
        observeThemeMode().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** `null` mientras se lee el estilo guardado. */
    val appStyle: StateFlow<AppStyle?> =
        observeAppStyle().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val permissionDenied = MutableStateFlow(false)

    val reminder: StateFlow<ReminderUiState> =
        combine(observeReminder(), permissionDenied) { settings, denied ->
            ReminderUiState(reminderSystem.supported, settings.enabled, settings.minutesOfDay, denied)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderUiState(reminderSystem.supported))

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }

    fun onAppStyleSelected(style: AppStyle) {
        viewModelScope.launch { setAppStyle(style) }
    }

    /** Al activar los avisos se pide antes el permiso de notificaciones; si se rechaza, quedan desactivados. */
    fun onReminderEnabledChange(enabled: Boolean) {
        if (enabled && !reminderSystem.notificationsAllowed()) {
            reminderSystem.requestNotifications { granted ->
                permissionDenied.value = !granted
                viewModelScope.launch { setReminderEnabled(granted) }
            }
        } else {
            permissionDenied.value = false
            viewModelScope.launch { setReminderEnabled(enabled) }
        }
    }

    fun onReminderTimeSelected(minutesOfDay: Int) {
        viewModelScope.launch { setReminderTime(minutesOfDay) }
    }
}
