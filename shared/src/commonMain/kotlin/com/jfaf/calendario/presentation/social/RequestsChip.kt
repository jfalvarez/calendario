package com.jfaf.calendario.presentation.social

import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.jfaf.calendario.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Chip de la fila de modos de la pantalla principal: indica solicitudes de compañeros pendientes.
 * Al pulsarlo se abre Ajustes, donde están los botones para aceptar, rechazar o bloquear.
 */
@Composable
fun RequestsChip(count: Int, onOpen: () -> Unit) {
    AssistChip(
        onClick = onOpen,
        label = { Text(stringResource(Res.string.social_chip, count)) },
    )
}
