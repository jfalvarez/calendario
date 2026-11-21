package com.jfaf.calendario.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.jfaf.calendario.data.local.CalendarDatabase
import com.jfaf.calendario.data.local.defaultShiftEntities
import com.jfaf.calendario.data.remote.RemoteDataSource
import com.jfaf.calendario.domain.repository.AuthRepository
import com.jfaf.calendario.domain.repository.ErrorReporter
import com.jfaf.calendario.domain.repository.NoOpErrorReporter
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Tablas que se sincronizan, en orden de subida y bajada. Para sincronizar una entidad nueva:
 * una clase `SyncTable` y una línea aquí.
 */
fun defaultSyncTables(db: CalendarDatabase, remote: RemoteDataSource): List<SyncTable> = listOf(
    ShiftSyncTable(db.shiftDao(), remote),
    AssignmentSyncTable(db.shiftAssignmentDao(), remote),
    HolidaySyncTable(db.localHolidayDao(), remote),
    DayNoteSyncTable(db.dayNoteDao(), remote),
)

/** Motor completo con sus tablas, estado y cambio de cuenta. Lo usan la app y las pruebas. */
@OptIn(ExperimentalTime::class)
fun createSyncEngine(
    db: CalendarDatabase,
    remote: RemoteDataSource,
    auth: AuthRepository,
    prefs: DataStore<Preferences>,
    scope: CoroutineScope,
    periodMs: Long = SyncEngine.DEFAULT_PERIOD_MS,
    debounceMs: Long = SyncEngine.DEBOUNCE_MS,
    clock: Clock = Clock.System,
    errors: ErrorReporter = NoOpErrorReporter,
): SyncEngine {
    val tables = defaultSyncTables(db, remote)
    val cursors = DataStoreSyncStateStore(prefs)
    val shiftDao = db.shiftDao()
    val adoption = AccountAdoption(tables, cursors) { shiftDao.upsertAll(defaultShiftEntities()) }
    return SyncEngine(tables, auth, adoption, cursors, scope, periodMs, debounceMs, clock, errors)
}
