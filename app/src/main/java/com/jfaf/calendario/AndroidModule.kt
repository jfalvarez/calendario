package com.jfaf.calendario

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.jfaf.calendario.data.prefs.USER_PREFERENCES_FILE
import com.jfaf.calendario.data.prefs.createPreferencesDataStore
import com.jfaf.calendario.data.local.CALENDAR_DATABASE_NAME
import com.jfaf.calendario.data.local.CalendarDatabase
import com.jfaf.calendario.data.local.buildCalendarDatabase
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
}
