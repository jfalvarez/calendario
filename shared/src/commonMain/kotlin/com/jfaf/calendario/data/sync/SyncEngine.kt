package com.jfaf.calendario.data.sync

import co.touchlab.kermit.Logger
import com.jfaf.calendario.data.remote.looksExpiredSession
import com.jfaf.calendario.data.remote.looksOffline
import com.jfaf.calendario.domain.model.AuthState
import com.jfaf.calendario.domain.model.SyncState
import com.jfaf.calendario.domain.repository.AuthRepository
import com.jfaf.calendario.domain.repository.ErrorReporter
import com.jfaf.calendario.domain.repository.NoOpErrorReporter
import com.jfaf.calendario.domain.repository.SyncRepository
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Sincronización offline-first: la interfaz sólo lee y escribe en Room; este motor orquesta las tablas
 * (`tables`): sube sus filas `dirty` (push) y descarga las novedades (pull) fusionándolas por «último cambio gana».
 * El motor no conoce las entidades: cada tabla sabe cómo sincronizarse (ver `SyncTables.kt`).
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class SyncEngine(
    private val tables: List<SyncTable>,
    private val auth: AuthRepository,
    private val adoption: AccountAdoption,
    private val cursors: SyncStateStore,
    private val scope: CoroutineScope,
    private val periodMs: Long = DEFAULT_PERIOD_MS,
    private val debounceMs: Long = DEBOUNCE_MS,
    private val clock: Clock = Clock.System,
    private val errors: ErrorReporter = NoOpErrorReporter,
) : SyncRepository {
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
        // Cambios locales pendientes en cualquier tabla → sincronizar tras una pausa (agrupa ediciones seguidas).
        launch {
            combine(tables.map { it.observeDirtyCount() }) { counts -> counts.sum() }
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

    /** Una pasada completa: adoptar cuenta, subir todas las tablas y luego descargar todas. Seguro frente a llamadas simultáneas. */
    suspend fun syncNow(userId: String) = mutex.withLock {
        _state.value = SyncState.Syncing
        try {
            adoption.adopt(userId)
            tables.forEach { it.push(userId) }
            tables.forEach { table ->
                table.pull(userId, cursors.cursor(table.name, userId))?.let { cursors.saveCursor(table.name, userId, it) }
            }
            lastSyncedAt = clock.now().toEpochMilliseconds()
            _state.value = SyncState.Idle(lastSyncedAt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            log.w(e) { "Fallo de sincronización" }
            if (looksExpiredSession(e)) {
                // El token no se renueva: la sesión ya no sirve. Se cierra para pedir entrar otra vez; no se informa como fallo.
                scope.launch { auth.signOut() }
                return@withLock
            }
            // Sin conexión es lo normal en un móvil; cualquier otro fallo sí interesa.
            if (!looksOffline(e)) errors.report(e, "sync")
            _state.value = SyncState.Failed(offline = looksOffline(e))
        }
    }

    companion object {
        const val DEFAULT_PERIOD_MS = 5 * 60 * 1000L
        const val DEBOUNCE_MS = 2_000L
    }
}

/** ¿Hay que volver a sincronizar? Sí si nunca se sincronizó o ya pasó `maxAgeMs` (o el reloj retrocedió). */
internal fun isStale(lastSyncedAt: Long?, now: Long, maxAgeMs: Long): Boolean =
    lastSyncedAt == null || now < lastSyncedAt || now - lastSyncedAt >= maxAgeMs
