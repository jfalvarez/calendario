package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.DayNote
import com.jfaf.calendario.domain.model.DayNoteDraft
import com.jfaf.calendario.domain.model.DayNoteError
import com.jfaf.calendario.domain.model.NoteKind
import com.jfaf.calendario.domain.repository.DayNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DayNoteUseCasesTest {
    private class FakeNotes : DayNoteRepository {
        val all = MutableStateFlow<List<DayNote>>(emptyList())
        override fun observeNotes(from: LocalDate, to: LocalDate): Flow<List<DayNote>> =
            all.map { list -> list.filter { it.date in from..to } }
        override fun observeDebts(): Flow<List<DayNote>> = all.map { list -> list.filter { it.isDebt }.sortedByDescending { it.date } }
        override suspend fun upsert(note: DayNote) { all.value = all.value.filter { it.id != note.id } + note }
        override suspend fun setSettled(id: String, settled: Boolean) {
            all.value = all.value.map { if (it.id == id) it.copy(settled = settled) else it }
        }
        override suspend fun delete(id: String) { all.value = all.value.filter { it.id != id } }
    }

    private val repo = FakeNotes()
    private var counter = 0
    private val save = SaveDayNoteUseCase(repo) { "id-${++counter}" }
    private val day = LocalDate(2025, 11, 12)

    private fun draft(kind: NoteKind, person: String = "", text: String = "", id: String? = null) =
        DayNoteDraft(id, day, kind, person, text)

    @Test
    fun plainNoteNeedsTextButNoPerson() = runTest {
        assertEquals(DayNoteError.TextRequired, save(draft(NoteKind.Note, text = "  ")))
        assertNull(save(draft(NoteKind.Note, text = " Curso de RCP ")))
        assertEquals("Curso de RCP", repo.all.value.single().text)
        assertNull(repo.all.value.single().person)
    }

    @Test
    fun debtNeedsAPersonButNoText() = runTest {
        assertEquals(DayNoteError.PersonRequired, save(draft(NoteKind.IOwe, person = " ")))
        assertNull(save(draft(NoteKind.OwedToMe, person = " Luis ")))
        val saved = repo.all.value.single()
        assertEquals("Luis", saved.person)
        assertEquals(NoteKind.OwedToMe, saved.kind)
    }

    @Test
    fun rejectsTooLongFields() = runTest {
        assertEquals(DayNoteError.PersonTooLong, save(draft(NoteKind.IOwe, person = "x".repeat(41))))
        assertEquals(DayNoteError.TextTooLong, save(draft(NoteKind.Note, text = "x".repeat(501))))
        assertTrue(repo.all.value.isEmpty())
    }

    @Test
    fun editingKeepsTheIdAndChangingToPlainNoteDropsSettled() = runTest {
        save(draft(NoteKind.IOwe, person = "Ana"))
        val id = repo.all.value.single().id
        repo.setSettled(id, true)
        save(DayNoteDraft(id, day, NoteKind.Note, "", "ya no hace falta", settled = true))
        val note = repo.all.value.single()
        assertEquals(id, note.id)
        assertEquals(false, note.settled)
    }

    @Test
    fun balancesGroupByPersonIgnoringCaseAndOrderPendingFirst() = runTest {
        save(draft(NoteKind.IOwe, person = "Ana"))
        save(draft(NoteKind.IOwe, person = "ana "))
        save(draft(NoteKind.OwedToMe, person = "Ana"))
        save(draft(NoteKind.OwedToMe, person = "Beto"))
        repo.setSettled(repo.all.value.first { it.person == "Beto" }.id, true)
        save(draft(NoteKind.OwedToMe, person = "Carlos"))

        val balances = ObserveDebtsUseCase(repo)().first()

        assertEquals(listOf("Ana", "Carlos", "Beto"), balances.map { it.person })
        assertEquals(2, balances[0].iOweCount)
        assertEquals(1, balances[0].owedToMeCount)
        assertEquals(1, balances[2].settled.size)
        assertEquals(0, balances[2].owedToMeCount)
    }

    @Test
    fun markersShowNotesAndOnlyOpenDebts() = runTest {
        save(draft(NoteKind.Note, text = "curso"))
        save(draft(NoteKind.IOwe, person = "Ana"))
        val markers = markersOf(repo.all.value)[day]!!
        assertTrue(markers.hasNote && markers.hasOpenDebt)

        repo.setSettled(repo.all.value.first { it.isDebt }.id, true)
        assertEquals(false, markersOf(repo.all.value)[day]!!.hasOpenDebt)
    }
}
