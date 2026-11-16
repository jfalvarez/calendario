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
import com.jfaf.calendario.domain.model.ThemeMode
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
    SettingsContent(mode ?: ThemeMode.System, onBack, viewModel::onThemeModeSelected, account, accountActions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    themeMode: ThemeMode,
    onBack: () -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    account: AccountUiState,
    accountActions: AccountActions,
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
