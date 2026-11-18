package com.jfaf.calendario.desktop

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.jfaf.calendario.data.local.CALENDAR_DATABASE_NAME
import com.jfaf.calendario.data.local.CalendarDatabase
import com.jfaf.calendario.data.local.buildCalendarDatabase
import com.jfaf.calendario.data.prefs.USER_PREFERENCES_FILE
import com.jfaf.calendario.data.prefs.createPreferencesDataStore
import com.jfaf.calendario.data.remote.AppPlatform
import com.jfaf.calendario.data.remote.FileSessionManager
import com.jfaf.calendario.data.remote.SupabaseConfig
import io.github.jan.supabase.auth.SessionManager
import com.jfaf.calendario.domain.repository.AppVersionProvider
import com.jfaf.calendario.domain.repository.ErrorReporter
import com.jfaf.calendario.presentation.settings.ReminderSystem
import com.jfaf.calendario.presentation.update.UpdateLauncher
import org.koin.dsl.module
import java.io.File
import java.awt.Desktop
import java.net.URI
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

/** `app-version.properties` lo genera la compilación a partir de `app.versionCode` (ver desktopApp/build.gradle.kts). */
fun loadAppVersion(): AppVersionProvider {
    val props = Properties()
    object {}.javaClass.classLoader.getResourceAsStream("app-version.properties")?.use { props.load(it) }
    return object : AppVersionProvider {
        override val versionCode = props.getProperty("version.code")?.toIntOrNull() ?: 0
        override val versionName = props.getProperty("version.name", "")
    }
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
    // La sesión de Supabase va a un archivo privado del usuario, no al registro de Windows (java.util.prefs).
    single<SessionManager> { FileSessionManager(File(dataDir, "session.json")) }
    single { AppPlatform("desktop") }
    single<ErrorReporter> { SentryErrorReporter() }
    // Los avisos de turno son sólo del móvil: en escritorio la sección se oculta.
    single<ReminderSystem> {
        object : ReminderSystem {
            override val supported = false
            override fun notificationsAllowed() = false
            override fun requestNotifications(onResult: (granted: Boolean) -> Unit) = onResult(false)
        }
    }
    single<AppVersionProvider> { loadAppVersion() }
    // Sin autoactualización: abre la página de descarga que fije la política (`store_url`).
    single<UpdateLauncher> {
        UpdateLauncher { policy ->
            val url = policy.storeUrl ?: return@UpdateLauncher
            runCatching { if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI(url)) }
        }
    }
}
