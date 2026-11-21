package com.jfaf.calendario.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.model.DayNoteDraft
import com.jfaf.calendario.domain.model.PersonBalance
import com.jfaf.calendario.domain.usecase.DeleteDayNoteUseCase
import com.jfaf.calendario.domain.usecase.ObserveDayNotesUseCase
import com.jfaf.calendario.domain.usecase.ObserveDebtsUseCase
import com.jfaf.calendario.domain.usecase.SaveDayNoteUseCase
import com.jfaf.calendario.domain.usecase.SetNoteSettledUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** Notas del día abierto (modo Notas). `null` = diálogo cerrado. */
@OptIn(ExperimentalCoroutinesApi::class)
class DayNotesViewModel(
    private val observeNotes: ObserveDayNotesUseCase,
    observeDebts: ObserveDebtsUseCase,
    private val saveNote: SaveDayNoteUseCase,
    private val setNoteSettled: SetNoteSettledUseCase,
    private val deleteNote: DeleteDayNoteUseCase,
) : ViewModel() {
    private val notesDate = MutableStateFlow<LocalDate?>(null)
    private val knownPeople = observeDebts().map { balances: List<PersonBalance> -> balances.map { it.person } }

    val uiState: StateFlow<NotesDialogState?> = notesDate.flatMapLatest { date ->
        if (date == null) flowOf(null)
        else combine(observeNotes(date, date), knownPeople) { notes, people -> NotesDialogState(date, notes, people) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onDayClick(date: LocalDate) { notesDate.value = date }
    fun onDismissNotes() { notesDate.value = null }

    /** Al cambiar de modo se cierra el diálogo de notas. */
    fun onReset() = onDismissNotes()

    /** El formulario ya valida antes de enviar; si aun así no fuera válido, simplemente no se guarda. */
    fun onSaveNote(draft: DayNoteDraft) { viewModelScope.launch { saveNote(draft) } }

    fun onSettleNote(id: String, settled: Boolean) { viewModelScope.launch { setNoteSettled(id, settled) } }

    fun onDeleteNote(id: String) { viewModelScope.launch { deleteNote(id) } }
}
