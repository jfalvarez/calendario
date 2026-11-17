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
import android.content.Intent
import androidx.core.net.toUri
import com.jfaf.calendario.domain.repository.AppVersionProvider
import com.jfaf.calendario.presentation.update.UpdateLauncher
import org.koin.android.ext.koin.androidContext
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
    // Abre la ficha de Play (o la URL de la política). Más adelante: Play In-App Updates (paso 5).
    single<UpdateLauncher> {
        val context = androidContext()
        UpdateLauncher { policy ->
            val uri = (policy.storeUrl ?: "market://details?id=${context.packageName}").toUri()
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
    single<AppVersionProvider> {
        object : AppVersionProvider {
            override val versionCode = BuildConfig.VERSION_CODE
            override val versionName = BuildConfig.VERSION_NAME
        }
    }
}
