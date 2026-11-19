package com.jfaf.calendario.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import com.jfaf.calendario.data.local.CalendarDatabase
import com.jfaf.calendario.data.local.defaultShiftEntities
import com.jfaf.calendario.data.remote.RemoteDataSource
import com.jfaf.calendario.data.remote.looksOffline
import com.jfaf.calendario.domain.model.AuthState
import com.jfaf.calendario.domain.model.SyncState
import com.jfaf.calendario.domain.repository.AuthRepository
import com.jfaf.calendario.domain.repository.ErrorReporter
import com.jfaf.calendario.domain.repository.NoOpErrorReporter
import com.jfaf.calendario.domain.repository.SyncRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Sincronización offline-first: la interfaz sólo lee y escribe en Room; este motor envía las filas
 * `dirty` (push) y descarga las novedades del servidor (pull) fusionándolas por «último cambio gana».
 * Ver docs 07-sincronizacion-supabase.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class SyncEngine(
    private val db: CalendarDatabase,
    private val remote: RemoteDataSource,
    private val auth: AuthRepository,
    private val prefs: DataStore<Preferences>,
    private val scope: CoroutineScope,
    private val periodMs: Long = DEFAULT_PERIOD_MS,
    private val debounceMs: Long = DEBOUNCE_MS,
    private val clock: Clock = Clock.System,
    private val errors: ErrorReporter = NoOpErrorReporter,
) : SyncRepository {
    private val shiftDao = db.shiftDao()
    private val assignmentDao = db.shiftAssignmentDao()
    private val holidayDao = db.localHolidayDao()
    private val noteDao = db.dayNoteDao()
    private val log = Logger.withTag("Sync")
    private val mutex = Mutex()
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val _state = MutableStateFlow<SyncState>(SyncState.NotSignedIn)
    @kotlin.concurrent.Volatile
    private var lastSyncedAt: Long? = null

    override val state: StateFlow<SyncState> = _state.asStateFlow()

    override fun requestSync() {
        requests.trySend(Unit)
    }

    override fun requestSyncIfStale() {
        if (isStale(lastSyncedAt, clock.now().toEpochMilliseconds(), SyncRepository.STALE_AFTER_MS)) requestSync()
    }

    /** Arranca el motor: se activa al iniciar sesión y se detiene al cerrarla. */
    fun start() {
        scope.launch {
            auth.authState.collectLatest { authState ->
                when (authState) {
                    is AuthState.SignedIn -> runWhileSignedIn(authState.userId)
                    AuthState.SignedOut -> _state.value = SyncState.NotSignedIn
                    AuthState.Loading -> Unit
                }
            }
        }
    }

    private suspend fun runWhileSignedIn(userId: String) = coroutineScope {
        _state.value = SyncState.Idle(lastSyncedAt)
        // Cambios locales pendientes → sincronizar tras una pausa (agrupa ediciones seguidas).
        launch {
            combine(
                shiftDao.observeDirtyCount(), assignmentDao.observeDirtyCount(),
                holidayDao.observeDirtyCount(), noteDao.observeDirtyCount(),
            ) { counts -> counts.sum() }
                .filter { it > 0 }
                .debounce(debounceMs)
                .collect { requestSync() }
        }
        // Descarga periódica mientras la app está abierta (sin tiempo real de momento).
        launch {
            while (true) {
                delay(periodMs)
                requestSync()
            }
        }
        requestSync()
        for (ignored in requests) syncNow(userId)
    }

    /** Una pasada completa: push y pull. Seguro frente a llamadas simultáneas. */
    suspend fun syncNow(userId: String) = mutex.withLock {
        _state.value = SyncState.Syncing
        try {
            adoptAccount(userId)
            push(userId)
            pull(userId)
            lastSyncedAt = clock.now().toEpochMilliseconds()
            _state.value = SyncState.Idle(lastSyncedAt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            log.w(e) { "Fallo de sincronización" }
            // Sin conexión es lo normal en un móvil; cualquier otro fallo sí interesa.
            if (!looksOffline(e)) errors.report(e, "sync")
            _state.value = SyncState.Failed(offline = looksOffline(e))
        }
    }

    /**
     * Si en este dispositivo entra una cuenta distinta de la última, se vacían los datos locales (eran de otro
     * usuario) para no subirlos a la cuenta nueva. La primera cuenta que inicia sesión adopta los datos locales.
     */
    private suspend fun adoptAccount(userId: String) {
        val last = prefs.data.first()[LAST_USER_KEY]
        if (last != null && last != userId) {
            assignmentDao.deleteAll()
            holidayDao.deleteAll()
            noteDao.deleteAll()
            shiftDao.deleteAll()
            shiftDao.upsertAll(defaultShiftEntities())
        }
        if (last != userId) prefs.edit { it[LAST_USER_KEY] = userId }
    }

    private suspend fun push(userId: String) {
        val shifts = shiftDao.getDirty()
        if (shifts.isNotEmpty()) {
            remote.upsertShifts(shifts.map { it.toRemote(userId) })
            shifts.forEach { shiftDao.markClean(it.id, it.updatedAt) }
        }
        val assignments = assignmentDao.getDirty()
        if (assignments.isNotEmpty()) {
            remote.upsertAssignments(assignments.map { it.toRemote(userId) })
            assignments.forEach { assignmentDao.markClean(it.id, it.updatedAt) }
        }
        val holidays = holidayDao.getDirty()
        if (holidays.isNotEmpty()) {
            remote.upsertHolidays(holidays.map { it.toRemote(userId) })
            holidays.forEach { holidayDao.markClean(it.date, it.updatedAt) }
        }
        val notes = noteDao.getDirty()
        if (notes.isNotEmpty()) {
            remote.upsertDayNotes(notes.map { it.toRemote(userId) })
            notes.forEach { noteDao.markClean(it.id, it.updatedAt) }
        }
    }

    private suspend fun pull(userId: String) {
        val shiftKey = stringPreferencesKey("sync_cursor_shifts_$userId")
        val assignmentKey = stringPreferencesKey("sync_cursor_assignments_$userId")

        val shifts = remote.pullShifts(prefs.data.first()[shiftKey])
        if (shifts.isNotEmpty()) {
            val local = shiftDao.getByIds(shifts.map { it.id }).associateBy { it.id }
            shiftDao.upsertAll(shifts.mapNotNull { mergeShift(local[it.id], it) })
            shifts.last().serverUpdatedAt?.let { cursor -> prefs.edit { it[shiftKey] = cursor } }
        }

        val assignments = remote.pullAssignments(prefs.data.first()[assignmentKey])
        if (assignments.isNotEmpty()) {
            val local = assignmentDao.getByIds(assignments.map { it.id }).associateBy { it.id }
            assignmentDao.upsertAll(assignments.mapNotNull { mergeAssignment(local[it.id], it) })
            assignments.last().serverUpdatedAt?.let { cursor -> prefs.edit { it[assignmentKey] = cursor } }
        }

        val holidayKey = stringPreferencesKey("sync_cursor_holidays_$userId")
        val holidays = remote.pullHolidays(prefs.data.first()[holidayKey])
        if (holidays.isNotEmpty()) {
            val local = holidayDao.getByDates(holidays.map { it.date }).associateBy { it.date }
            holidayDao.upsertAll(holidays.mapNotNull { mergeHoliday(local[it.date], it) })
            holidays.last().serverUpdatedAt?.let { cursor -> prefs.edit { it[holidayKey] = cursor } }
        }

        val noteKey = stringPreferencesKey("sync_cursor_day_notes_$userId")
        val notes = remote.pullDayNotes(prefs.data.first()[noteKey])
        if (notes.isNotEmpty()) {
            val local = noteDao.getByIds(notes.map { it.id }).associateBy { it.id }
            noteDao.upsertAll(notes.mapNotNull { mergeDayNote(local[it.id], it) })
            notes.last().serverUpdatedAt?.let { cursor -> prefs.edit { it[noteKey] = cursor } }
        }
    }

    companion object {
        const val DEFAULT_PERIOD_MS = 5 * 60 * 1000L
        const val DEBOUNCE_MS = 2_000L
        private val LAST_USER_KEY = stringPreferencesKey("sync_last_user_id")
    }
}

/** ¿Hay que volver a sincronizar? Sí si nunca se sincronizó o ya pasó `maxAgeMs` (o el reloj retrocedió). */
internal fun isStale(lastSyncedAt: Long?, now: Long, maxAgeMs: Long): Boolean =
    lastSyncedAt == null || now < lastSyncedAt || now - lastSyncedAt >= maxAgeMs
