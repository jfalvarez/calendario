package com.jfaf.calendario.presentation.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jfaf.calendario.domain.model.AppVersionPolicy
import com.jfaf.calendario.domain.model.UpdateStatus
import com.jfaf.calendario.domain.platform.UpdateLauncher
import com.jfaf.calendario.resources.Res
import com.jfaf.calendario.resources.update_later
import com.jfaf.calendario.resources.update_now
import com.jfaf.calendario.resources.update_recommended_message
import com.jfaf.calendario.resources.update_recommended_title
import com.jfaf.calendario.resources.update_required_message
import com.jfaf.calendario.resources.update_required_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Envuelve la navegación. `Required` sustituye todo el contenido por una pantalla sin salida (ni siquiera hay
 * navegación detrás, así que el botón Atrás no la salta); `Recommended` añade un diálogo descartable.
 */
@Composable
fun UpdateGate(content: @Composable () -> Unit) {
    val viewModel: UpdateViewModel = koinViewModel()
    val launcher: UpdateLauncher = koinInject()
    val status by viewModel.status.collectAsState()
    when (val s = status) {
        is UpdateStatus.Required -> RequiredUpdateScreen(s.policy) { launcher.startUpdate(s.policy) }
        is UpdateStatus.Recommended -> {
            content()
            RecommendedUpdateDialog(
                policy = s.policy,
                onUpdate = { launcher.startUpdate(s.policy) },
                onLater = viewModel::onDismissRecommended,
            )
        }
        UpdateStatus.UpToDate, UpdateStatus.Unknown -> content()
    }
}

@Composable
private fun RequiredUpdateScreen(policy: AppVersionPolicy, onUpdate: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.update_required_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(policy.message ?: stringResource(Res.string.update_required_message), textAlign = TextAlign.Center)
            Button(onClick = onUpdate) { Text(stringResource(Res.string.update_now)) }
        }
    }
}

@Composable
private fun RecommendedUpdateDialog(policy: AppVersionPolicy, onUpdate: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(Res.string.update_recommended_title)) },
        text = { Text(policy.message ?: stringResource(Res.string.update_recommended_message)) },
        confirmButton = { Button(onClick = onUpdate) { Text(stringResource(Res.string.update_now)) } },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(Res.string.update_later)) } },
    )
}
