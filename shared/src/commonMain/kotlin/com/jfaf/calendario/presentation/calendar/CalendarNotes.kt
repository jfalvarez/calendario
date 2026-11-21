package com.jfaf.calendario.presentation.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.jfaf.calendario.domain.model.DayNote
import com.jfaf.calendario.domain.model.DayNoteDraft
import com.jfaf.calendario.domain.model.NoteKind
import com.jfaf.calendario.resources.Res
import com.jfaf.calendario.resources.cancel
import com.jfaf.calendario.resources.debt_mark_settled
import com.jfaf.calendario.resources.debt_reopen
import com.jfaf.calendario.resources.note_add
import com.jfaf.calendario.resources.note_close
import com.jfaf.calendario.resources.note_debt_i_owe
import com.jfaf.calendario.resources.note_debt_owed_to_me
import com.jfaf.calendario.resources.note_delete
import com.jfaf.calendario.resources.note_edit
import com.jfaf.calendario.resources.note_kind_i_owe
import com.jfaf.calendario.resources.note_kind_note
import com.jfaf.calendario.resources.note_kind_owed_to_me
import com.jfaf.calendario.resources.note_person_i_owe
import com.jfaf.calendario.resources.note_person_owed_to_me
import com.jfaf.calendario.resources.note_text
import com.jfaf.calendario.resources.note_text_optional
import com.jfaf.calendario.resources.notes_dialog_title
import com.jfaf.calendario.resources.notes_empty
import com.jfaf.calendario.resources.save
import org.jetbrains.compose.resources.stringResource

/** Notas y deudas de un día: lista con acciones y, al añadir o editar, el formulario. */
@Composable
internal fun NotesDialog(
    state: NotesDialogState,
    monthName: String,
    onDismiss: () -> Unit,
    onSave: (DayNoteDraft) -> Unit,
    onSettle: (id: String, settled: Boolean) -> Unit,
    onDelete: (id: String) -> Unit,
) {
    var editing by remember(state.date) { mutableStateOf<DayNoteDraft?>(null) }
    val draft = editing
    val title = stringResource(Res.string.notes_dialog_title, state.date.day, monthName)

    if (draft == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    if (state.notes.isEmpty()) Text(stringResource(Res.string.notes_empty))
                    state.notes.forEach { note ->
                        NoteRow(
                            note = note,
                            onSettle = { onSettle(note.id, it) },
                            onEdit = { editing = DayNoteDraft(note.id, note.date, note.kind, note.person.orEmpty(), note.text, note.settled) },
                            onDelete = { onDelete(note.id) },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editing = DayNoteDraft(null, state.date, NoteKind.Note, "", "") }) {
                    Text(stringResource(Res.string.note_add))
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.note_close)) } },
        )
    } else {
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(title) },
            text = { NoteForm(draft, state.knownPeople) { editing = it } },
            confirmButton = {
                TextButton(
                    onClick = { onSave(draft); editing = null },
                    enabled = draft.validationError() == null,
                ) { Text(stringResource(Res.string.save)) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun NoteRow(note: DayNote, onSettle: (Boolean) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val person = note.person.orEmpty()
    val headline = when (note.kind) {
        NoteKind.Note -> note.text
        NoteKind.IOwe -> stringResource(Res.string.note_debt_i_owe, person)
        NoteKind.OwedToMe -> stringResource(Res.string.note_debt_owed_to_me, person)
    }
    val faded = note.isDebt && note.settled
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (note.isDebt) Icons.Filled.SwapHoriz else Icons.AutoMirrored.Filled.StickyNote2, null,
            Modifier.size(20.dp).alpha(if (faded) 0.5f else 1f),
        )
        Column(Modifier.weight(1f).padding(horizontal = 8.dp).alpha(if (faded) 0.5f else 1f)) {
            Text(
                headline, style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (faded) TextDecoration.LineThrough else null,
            )
            if (note.isDebt && note.text.isNotBlank()) {
                Text(note.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (note.isDebt) {
            Checkbox(
                checked = note.settled, onCheckedChange = onSettle,
                modifier = Modifier.size(40.dp),
            )
        }
        IconButton(onClick = onEdit, Modifier.size(40.dp)) { Icon(Icons.Filled.Edit, stringResource(Res.string.note_edit)) }
        IconButton(onClick = onDelete, Modifier.size(40.dp)) { Icon(Icons.Filled.Delete, stringResource(Res.string.note_delete)) }
    }
}

@Composable
private fun NoteForm(draft: DayNoteDraft, knownPeople: List<String>, onChange: (DayNoteDraft) -> Unit) {
    val isDebt = draft.kind != NoteKind.Note
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                NoteKind.Note to Res.string.note_kind_note,
                NoteKind.IOwe to Res.string.note_kind_i_owe,
                NoteKind.OwedToMe to Res.string.note_kind_owed_to_me,
            ).forEach { (kind, label) ->
                FilterChip(
                    selected = draft.kind == kind,
                    onClick = { onChange(draft.copy(kind = kind)) },
                    label = { Text(stringResource(label)) },
                )
            }
        }
        if (isDebt) {
            OutlinedTextField(
                value = draft.person,
                onValueChange = { onChange(draft.copy(person = it)) },
                label = {
                    Text(stringResource(if (draft.kind == NoteKind.IOwe) Res.string.note_person_i_owe else Res.string.note_person_owed_to_me))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            // Personas ya usadas que encajan con lo escrito: un toque en vez de teclear de nuevo.
            val typed = draft.person.trim()
            val suggestions = knownPeople.filter { !it.equals(typed, ignoreCase = true) && it.contains(typed, ignoreCase = true) }.take(5)
            if (suggestions.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { name ->
                        AssistChip(onClick = { onChange(draft.copy(person = name)) }, label = { Text(name) })
                    }
                }
            }
        }
        OutlinedTextField(
            value = draft.text,
            onValueChange = { onChange(draft.copy(text = it)) },
            label = { Text(stringResource(if (isDebt) Res.string.note_text_optional else Res.string.note_text)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        if (isDebt && draft.id != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = draft.settled, onCheckedChange = { onChange(draft.copy(settled = it)) })
                Text(stringResource(if (draft.settled) Res.string.debt_reopen else Res.string.debt_mark_settled))
            }
        }
    }
}
