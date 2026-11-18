package com.jfaf.calendario.presentation.debts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.jfaf.calendario.domain.model.DayNote
import com.jfaf.calendario.domain.model.NoteKind
import com.jfaf.calendario.domain.model.PersonBalance
import com.jfaf.calendario.resources.Res
import com.jfaf.calendario.resources.back
import com.jfaf.calendario.resources.debts_all_settled
import com.jfaf.calendario.resources.debts_empty
import com.jfaf.calendario.resources.debts_owed_to_you
import com.jfaf.calendario.resources.debts_settled_section
import com.jfaf.calendario.resources.debts_title
import com.jfaf.calendario.resources.debts_you_owe
import com.jfaf.calendario.resources.month_names
import com.jfaf.calendario.resources.note_kind_i_owe
import com.jfaf.calendario.resources.note_kind_owed_to_me
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DebtsScreen(onBack: () -> Unit, viewModel: DebtsViewModel = koinViewModel()) {
    val balances by viewModel.balances.collectAsState()
    DebtsContent(balances, onBack, viewModel::onSettle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsContent(balances: List<PersonBalance>?, onBack: () -> Unit, onSettle: (id: String, settled: Boolean) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.debts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            balances == null -> Unit
            balances.isEmpty() -> Text(
                stringResource(Res.string.debts_empty),
                Modifier.padding(padding).padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(balances, key = { it.person.lowercase() }) { PersonCard(it, onSettle) }
            }
        }
    }
}

@Composable
private fun PersonCard(balance: PersonBalance, onSettle: (String, Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(balance.person, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            val summary = buildList {
                if (balance.iOweCount > 0) add(stringResource(Res.string.debts_you_owe, balance.iOweCount))
                if (balance.owedToMeCount > 0) add(stringResource(Res.string.debts_owed_to_you, balance.owedToMeCount))
            }
            Text(
                if (summary.isEmpty()) stringResource(Res.string.debts_all_settled) else summary.joinToString(" · "),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            balance.open.forEach { DebtRow(it, onSettle) }
            if (balance.settled.isNotEmpty()) {
                Text(
                    stringResource(Res.string.debts_settled_section),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                balance.settled.forEach { DebtRow(it, onSettle) }
            }
        }
    }
}

@Composable
private fun DebtRow(note: DayNote, onSettle: (String, Boolean) -> Unit) {
    val months = stringArrayResource(Res.array.month_names)
    val kind = stringResource(if (note.kind == NoteKind.IOwe) Res.string.note_kind_i_owe else Res.string.note_kind_owed_to_me)
    val date = "${note.date.day} ${months[note.date.month.ordinal].take(3).lowercase()} ${note.date.year}"
    Row(Modifier.fillMaxWidth().alpha(if (note.settled) 0.55f else 1f), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                "$kind · $date", style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (note.settled) TextDecoration.LineThrough else null,
            )
            if (note.text.isNotBlank()) {
                Text(note.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Checkbox(checked = note.settled, onCheckedChange = { onSettle(note.id, it) })
    }
}
