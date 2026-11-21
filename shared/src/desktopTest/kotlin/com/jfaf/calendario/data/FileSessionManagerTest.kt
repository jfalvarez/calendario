package com.jfaf.calendario.data

import com.jfaf.calendario.data.remote.FileSessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class FileSessionManagerTest {
    private lateinit var dir: File
    private lateinit var file: File

    @BeforeTest
    fun setUp() {
        dir = File.createTempFile("session-test", "").apply { delete(); mkdirs() }
        file = File(dir, "session.json")
    }

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun session(expiresAt: Instant) = UserSession(
        accessToken = "access",
        refreshToken = "refresh",
        expiresIn = 3600,
        tokenType = "bearer",
        expiresAt = expiresAt,
    )

    @Test
    fun keepsTheExpiryDateAcrossSaveAndLoad() = runTest {
        val expiry = Instant.fromEpochSeconds(2_000_000_000)
        val manager = FileSessionManager(file)
        manager.saveSession(session(expiry))
        assertEquals(expiry, manager.loadSessionOrNull()?.expiresAt)
    }

    @Test
    fun aSessionWithoutExpiryDateIsDiscarded() = runTest {
        // Formato anterior: sin expiresAt. Al cargarla la librería la daría por válida una hora más.
        file.writeText("""{"access_token":"a","refresh_token":"r","expires_in":3600,"token_type":"bearer"}""")
        val manager = FileSessionManager(file)
        assertNull(manager.loadSessionOrNull())
        assertFalse(file.exists())
    }
}
