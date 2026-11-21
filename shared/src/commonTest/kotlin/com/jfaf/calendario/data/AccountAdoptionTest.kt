package com.jfaf.calendario.data

import com.jfaf.calendario.data.sync.AccountAdoption
import com.jfaf.calendario.fakes.InMemorySyncStateStore
import com.jfaf.calendario.fakes.RecordingSyncTable
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountAdoptionTest {
    private val events = mutableListOf<String>()
    private val store = InMemorySyncStateStore()
    private val shifts = RecordingSyncTable("shifts", events)
    private val notes = RecordingSyncTable("day_notes", events)
    private var seeded = 0
    private val adoption = AccountAdoption(listOf(shifts, notes), store) { seeded++ }

    @Test
    fun firstAccountAdoptsLocalDataWithoutClearing() = runTest {
        adoption.adopt("ana")
        assertEquals(0, shifts.cleared + notes.cleared)
        assertEquals(0, seeded)
        assertEquals("ana", store.lastUserId())
    }

    @Test
    fun sameAccountKeepsLocalData() = runTest {
        store.saveLastUserId("ana")
        adoption.adopt("ana")
        assertEquals(0, shifts.cleared + notes.cleared)
        assertEquals(0, seeded)
    }

    @Test
    fun otherAccountClearsEveryTableAndSeedsDefaults() = runTest {
        store.saveLastUserId("ana")
        adoption.adopt("luis")
        assertEquals(listOf("clear:shifts", "clear:day_notes"), events)
        assertEquals(1, seeded)
        assertEquals("luis", store.lastUserId())
    }
}
