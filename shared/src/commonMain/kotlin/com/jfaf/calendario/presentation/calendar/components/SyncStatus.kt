package com.jfaf.calendario.presentation.calendar.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.jfaf.calendario.presentation.calendar.*
import com.jfaf.calendario.presentation.common.color
import com.jfaf.calendario.resources.*
import org.jetbrains.compose.resources.stringResource

/** Icono y texto breve del estado de sincronización; al tocarlo se sincroniza. */
@Composable
internal fun SyncStatus(indicator: SyncIndicator, onClick: () -> Unit) {
    val label = stringResource(
        when (indicator) {
            SyncIndicator.Pending -> Res.string.sync_status_pending
            SyncIndicator.Syncing -> Res.string.sync_status_syncing
            SyncIndicator.Synced -> Res.string.sync_status_synced
            SyncIndicator.Offline -> Res.string.sync_status_offline
            SyncIndicator.Error -> Res.string.sync_status_error
        }
    )
    val problem = indicator == SyncIndicator.Offline || indicator == SyncIndicator.Error
    val color = if (problem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = indicator != SyncIndicator.Syncing, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (indicator == SyncIndicator.Syncing) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = color)
        } else {
            Icon(
                when (indicator) {
                    SyncIndicator.Synced -> Icons.Filled.CloudDone
                    SyncIndicator.Offline -> Icons.Filled.CloudOff
                    SyncIndicator.Error -> Icons.Filled.SyncProblem
                    else -> Icons.Filled.CloudQueue
                },
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}
