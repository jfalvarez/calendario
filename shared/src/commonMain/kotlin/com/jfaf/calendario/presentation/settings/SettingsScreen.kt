package com.jfaf.calendario.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jfaf.calendario.resources.accept
import com.jfaf.calendario.resources.cancel
import com.jfaf.calendario.resources.reminder_enable
import com.jfaf.calendario.resources.reminder_enable_desc
import com.jfaf.calendario.resources.reminder_permission_denied
import com.jfaf.calendario.resources.reminder_pick_time
import com.jfaf.calendario.resources.reminder_time
import com.jfaf.calendario.resources.reminders_title
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jfaf.calendario.domain.model.AppStyle
import com.jfaf.calendario.domain.model.ThemeMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.jfaf.calendario.resources.app_style
import com.jfaf.calendario.resources.style_pop
import com.jfaf.calendario.resources.style_pop_desc
import com.jfaf.calendario.resources.style_sorbete
import com.jfaf.calendario.resources.style_sorbete_desc
import com.jfaf.calendario.resources.style_neon
import com.jfaf.calendario.resources.style_neon_desc
import com.jfaf.calendario.resources.Res
import com.jfaf.calendario.resources.back
import com.jfaf.calendario.resources.settings_title
import com.jfaf.calendario.resources.theme
import com.jfaf.calendario.resources.theme_dark
import com.jfaf.calendario.resources.theme_light
import com.jfaf.calendario.resources.theme_system
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
    accountViewModel: AccountViewModel = koinViewModel(),
) {
    val account by accountViewModel.uiState.collectAsState()
    val accountActions = remember(accountViewModel) {
        AccountActions(
            accountViewModel::onEmailChange, accountViewModel::onPasswordChange, accountViewModel::onSignIn,
            accountViewModel::onSignUp, accountViewModel::onSignOut, accountViewModel::onSyncNow,
        )
    }
    val mode by viewModel.themeMode.collectAsState()
    val style by viewModel.appStyle.collectAsState()
    val reminder by viewModel.reminder.collectAsState()
    SettingsContent(
        mode ?: ThemeMode.System, style ?: AppStyle.Pop, onBack,
        viewModel::onThemeModeSelected, viewModel::onAppStyleSelected, account, accountActions,
        reminder, viewModel::onReminderEnabledChange, viewModel::onReminderTimeSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    themeMode: ThemeMode,
    appStyle: AppStyle,
    onBack: () -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onAppStyleSelected: (AppStyle) -> Unit,
    account: AccountUiState,
    accountActions: AccountActions,
    reminder: ReminderUiState = ReminderUiState(),
    onReminderEnabledChange: (Boolean) -> Unit = {},
    onReminderTimeSelected: (Int) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AccountSection(account, accountActions)

            if (reminder.supported) ReminderSection(reminder, onReminderEnabledChange, onReminderTimeSelected)

            Text(stringResource(Res.string.app_style), style = MaterialTheme.typography.titleMedium)
            AppStyle.entries.forEach { StyleCard(it, it == appStyle, onAppStyleSelected) }

            Text(stringResource(Res.string.theme), style = MaterialTheme.typography.titleMedium)
            ThemeOption(Res.string.theme_system, ThemeMode.System, themeMode, onThemeModeSelected)
            ThemeOption(Res.string.theme_light, ThemeMode.Light, themeMode, onThemeModeSelected)
            ThemeOption(Res.string.theme_dark, ThemeMode.Dark, themeMode, onThemeModeSelected)
        }
    }
}

@Composable
private fun ThemeOption(label: StringResource, option: ThemeMode, selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onSelect(option) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = option == selected, onClick = { onSelect(option) })
        Text(stringResource(label))
    }
}

private class StylePreview(val name: StringResource, val desc: StringResource, val bg: Color, val dots: List<Color>)

private fun previewOf(style: AppStyle) = when (style) {
    AppStyle.Pop -> StylePreview(
        Res.string.style_pop, Res.string.style_pop_desc, Color(0xFFFFF1F7),
        listOf(Color(0xFF7B2FF7), Color(0xFFFF6B9D), Color(0xFFFFC93C), Color(0xFF3DD9B0)),
    )
    AppStyle.Sorbete -> StylePreview(
        Res.string.style_sorbete, Res.string.style_sorbete_desc, Color(0xFFF2F6FF),
        listOf(Color(0xFF4F6BFF), Color(0xFFB9B5FF), Color(0xFFFFB8D0), Color(0xFFB7EBD3)),
    )
    AppStyle.Neon -> StylePreview(
        Res.string.style_neon, Res.string.style_neon_desc, Color(0xFF0F0F17),
        listOf(Color(0xFFC6FF3D), Color(0xFF6C8CFF), Color(0xFFFF5C8A), Color(0xFFFFB020)),
    )
}

/** Tarjeta seleccionable con una vista previa de la paleta del estilo (fija, no depende del tema activo). */
@Composable
private fun StyleCard(style: AppStyle, selected: Boolean, onSelect: (AppStyle) -> Unit) {
    val p = previewOf(style)
    Surface(
        onClick = { onSelect(style) },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (selected) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(width = 84.dp, height = 56.dp).clip(MaterialTheme.shapes.medium).background(p.bg),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    p.dots.forEach { Box(Modifier.size(14.dp).background(it, CircleShape)) }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(p.name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(p.desc), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RadioButton(selected = selected, onClick = { onSelect(style) })
        }
    }
}

/** Interruptor de avisos y, si están activos, la hora a la que se emiten (la víspera del turno). */
@Composable
private fun ReminderSection(
    state: ReminderUiState,
    onEnabledChange: (Boolean) -> Unit,
    onTimeSelected: (Int) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.reminders_title), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.reminder_enable))
                Text(
                    stringResource(Res.string.reminder_enable_desc), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = state.enabled, onCheckedChange = onEnabledChange)
        }
        if (state.permissionDenied) {
            Text(
                stringResource(Res.string.reminder_permission_denied), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.enabled) {
            Row(
                Modifier.fillMaxWidth().clickable { picking = true }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(Res.string.reminder_time), Modifier.weight(1f))
                Text(formatClock(state.minutesOfDay), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
    if (picking) {
        TimePickerDialog(
            initialMinutes = state.minutesOfDay,
            onConfirm = { picking = false; onTimeSelected(it) },
            onDismiss = { picking = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initialMinutes: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberTimePickerState(initialMinutes / 60, initialMinutes % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.reminder_pick_time)) },
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerState.hour * 60 + pickerState.minute) }) {
                Text(stringResource(Res.string.accept))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}

private fun formatClock(minutesOfDay: Int): String =
    "${(minutesOfDay / 60).toString().padStart(2, '0')}:${(minutesOfDay % 60).toString().padStart(2, '0')}"
