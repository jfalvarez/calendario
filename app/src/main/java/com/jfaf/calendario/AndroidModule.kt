package com.jfaf.calendario

import androidx.room.Room
import com.jfaf.calendario.data.local.CALENDAR_DATABASE_NAME
import com.jfaf.calendario.data.local.CalendarDatabase
import com.jfaf.calendario.data.local.buildCalendarDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single {
        val file = androidContext().getDatabasePath(CALENDAR_DATABASE_NAME).also { it.parentFile?.mkdirs() }
        buildCalendarDatabase(Room.databaseBuilder<CalendarDatabase>(androidContext(), file.absolutePath))
    }
}
