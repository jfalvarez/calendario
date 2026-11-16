package com.jfaf.calendario.data

import com.jfaf.calendario.data.prefs.DataStoreSettingsRepository
import com.jfaf.calendario.data.prefs.USER_PREFERENCES_FILE
import com.jfaf.calendario.data.prefs.createPreferencesDataStore
import com.jfaf.calendario.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryTest {
    @Test
    fun defaultsToSystemAndPersistsChoiceInNewFolder() = runTest {
        val dir = kotlin.io.path.createTempDirectory("calendario-prefs").toFile()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        try {
            // La subcarpeta `datastore` aún no existe: DataStore debe crearla.
            val repo = DataStoreSettingsRepository(
                createPreferencesDataStore(File(dir, "datastore/$USER_PREFERENCES_FILE").absolutePath, scope)
            )
            assertEquals(ThemeMode.System, repo.observeThemeMode().first())
            repo.setThemeMode(ThemeMode.Dark)
            assertEquals(ThemeMode.Dark, repo.observeThemeMode().first())
        } finally {
            scope.cancel()
            dir.deleteRecursively()
        }
    }
}
