package com.jfaf.calendario.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.model.ThemeMode
import com.jfaf.calendario.domain.usecase.ObserveThemeModeUseCase
import com.jfaf.calendario.domain.usecase.SetThemeModeUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
) : ViewModel() {
    /** `null` mientras se leen los ajustes (evita un destello con el tema equivocado al arrancar). */
    val themeMode: StateFlow<ThemeMode?> =
        observeThemeMode().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }
}
