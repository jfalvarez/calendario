package com.jfaf.calendario.desktop

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.jfaf.calendario.data.local.CALENDAR_DATABASE_NAME
import com.jfaf.calendario.data.local.CalendarDatabase
import com.jfaf.calendario.data.local.buildCalendarDatabase
import com.jfaf.calendario.data.prefs.USER_PREFERENCES_FILE
import com.jfaf.calendario.data.prefs.createPreferencesDataStore
import com.jfaf.calendario.data.remote.SupabaseConfig
import org.koin.dsl.module
import java.io.File
import java.util.Properties

/** Carpeta de datos del usuario: `%APPDATA%\Calendario` en Windows, `~/.local/share/calendario` en el resto. */
fun appDataDir(): File {
    val appData = System.getenv("APPDATA")
    val dir = if (appData != null) File(appData, "Calendario")
    else File(System.getProperty("user.home"), ".local/share/calendario")
    return dir.also { it.mkdirs() }
}

/** `supabase.properties` lo genera la compilación a partir de `local.properties` (ver desktopApp/build.gradle.kts). */
fun loadSupabaseConfig(): SupabaseConfig {
    val props = Properties()
    object {}.javaClass.classLoader.getResourceAsStream("supabase.properties")?.use { props.load(it) }
    return SupabaseConfig(props.getProperty("supabase.url", ""), props.getProperty("supabase.anonKey", ""))
}

fun desktopModule(dataDir: File = appDataDir()) = module {
    single<DataStore<Preferences>> {
        createPreferencesDataStore(File(dataDir, "datastore/$USER_PREFERENCES_FILE").absolutePath)
    }
    single {
        buildCalendarDatabase(
            Room.databaseBuilder<CalendarDatabase>(name = File(dataDir, CALENDAR_DATABASE_NAME).absolutePath)
        )
    }
    single { loadSupabaseConfig() }
}
