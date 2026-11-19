package com.jfaf.calendario.data

import com.jfaf.calendario.data.sync.AccountAdoption
import com.jfaf.calendario.data.sync.SyncEngine
import com.jfaf.calendario.domain.model.SyncState
import com.jfaf.calendario.fakes.FakeAuthRepository
import com.jfaf.calendario.fakes.InMemorySyncStateStore
import com.jfaf.calendario.fakes.RecordingSyncTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** El motor trata las tablas de forma genérica: no depende de cuántas ni de cuáles sean. */
class SyncEngineTablesTest {
    private val events = mutableListOf<String>()
    private val store = InMemorySyncStateStore()

    private fun engine(vararg tables: RecordingSyncTable): SyncEngine {
        val list = tables.toList()
        return SyncEngine(
            tables = list,
            auth = FakeAuthRepository(),
            adoption = AccountAdoption(list, store) {},
            cursors = store,
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
    }

    @Test
    fun pushesAllTablesThenPullsEachOneInOrder() = runTest {
        val shifts = RecordingSyncTable("shifts", events, pullResult = "c-shifts")
        val notes = RecordingSyncTable("day_notes", events)
        engine(shifts, notes).syncNow("ana")
        assertEquals(
            listOf("push:shifts", "push:day_notes", "pull:shifts:-", "pull:day_notes:-"),
            events,
        )
        assertEquals("c-shifts", store.cursor("shifts", "ana"))
        assertEquals(null, store.cursor("day_notes", "ana"))
    }

    @Test
    fun savedCursorIsUsedOnTheNextPull() = runTest {
        val shifts = RecordingSyncTable("shifts", events, pullResult = "c1")
        val sync = engine(shifts)
        sync.syncNow("ana")
        events.clear()
        sync.syncNow("ana")
        assertEquals(listOf("push:shifts", "pull:shifts:c1"), events)
    }

    @Test
    fun aNewTableIsSyncedWithoutChangingTheEngine() = runTest {
        val a = RecordingSyncTable("a", events, pullResult = "ca")
        val b = RecordingSyncTable("b", events)
        val fresh = RecordingSyncTable("entidad_nueva", events, pullResult = "cn")
        val sync = engine(a, b, fresh)
        sync.syncNow("ana")
        assertEquals("cn", store.cursor("entidad_nueva", "ana"))
        assertEquals(listOf("push:a", "push:b", "push:entidad_nueva"), events.take(3))
        assertIs<SyncState.Idle>(sync.state.value)
    }
}
