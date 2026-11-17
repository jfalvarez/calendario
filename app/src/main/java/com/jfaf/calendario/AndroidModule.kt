package com.jfaf.calendario

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.jfaf.calendario.data.local.CALENDAR_DATABASE_NAME
import com.jfaf.calendario.data.local.CalendarDatabase
import com.jfaf.calendario.data.local.buildCalendarDatabase
import com.jfaf.calendario.data.prefs.USER_PREFERENCES_FILE
import com.jfaf.calendario.data.prefs.createPreferencesDataStore
import com.jfaf.calendario.data.remote.AppPlatform
import com.jfaf.calendario.data.remote.SupabaseConfig
import com.jfaf.calendario.domain.repository.AppVersionProvider
import com.jfaf.calendario.crash.SentryErrorReporter
import com.jfaf.calendario.domain.repository.ErrorReporter
import com.jfaf.calendario.presentation.update.UpdateLauncher
import com.jfaf.calendario.widget.ShiftWidgetUpdater
import com.jfaf.calendario.presentation.settings.ReminderSystem
import com.jfaf.calendario.reminder.AndroidReminderSystem
import com.jfaf.calendario.reminder.ReminderScheduler
import com.jfaf.calendario.reminder.ReminderUpdater
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val androidModule = module {
    single<DataStore<Preferences>> {
        createPreferencesDataStore(androidContext().filesDir.resolve("datastore/$USER_PREFERENCES_FILE").absolutePath)
    }
    single {
        val file = androidContext().getDatabasePath(CALENDAR_DATABASE_NAME).also { it.parentFile?.mkdirs() }
        buildCalendarDatabase(Room.databaseBuilder<CalendarDatabase>(androidContext(), file.absolutePath))
    }
    // La URL y la clave pública salen de local.properties en la compilación (ver app/build.gradle.kts).
    single { SupabaseConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) }
    single { AppPlatform("android") }
    single { ShiftWidgetUpdater(androidContext(), get(), get()) }
    single { AndroidReminderSystem(androidContext()) } bind ReminderSystem::class
    single { ReminderScheduler(androidContext(), get()) }
    single<ErrorReporter> { SentryErrorReporter() }
    single { ReminderUpdater(get(), get(), get(), get(), get()) }
    // Play In-App Updates con la ficha de la tienda como plan B (ver PlayUpdateLauncher).
    single { PlayUpdateLauncher(androidContext(), get()) } bind UpdateLauncher::class
    single<AppVersionProvider> {
        object : AppVersionProvider {
            override val versionCode = BuildConfig.VERSION_CODE
            override val versionName = BuildConfig.VERSION_NAME
        }
    }
}
