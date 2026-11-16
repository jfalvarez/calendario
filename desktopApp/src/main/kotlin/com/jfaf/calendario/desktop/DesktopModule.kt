package com.jfaf.calendario.desktop

import androidx.room.Room
import com.jfaf.calendario.data.local.CALENDAR_DATABASE_NAME
import com.jfaf.calendario.data.local.CalendarDatabase
import com.jfaf.calendario.data.local.buildCalendarDatabase
import org.koin.dsl.module
import java.io.File

/** Carpeta de datos del usuario: `%APPDATA%\Calendario` en Windows, `~/.local/share/calendario` en el resto. */
fun appDataDir(): File {
    val appData = System.getenv("APPDATA")
    val dir = if (appData != null) File(appData, "Calendario")
    else File(System.getProperty("user.home"), ".local/share/calendario")
    return dir.also { it.mkdirs() }
}

fun desktopModule(dataDir: File = appDataDir()) = module {
    single {
        buildCalendarDatabase(
            Room.databaseBuilder<CalendarDatabase>(name = File(dataDir, CALENDAR_DATABASE_NAME).absolutePath)
        )
    }
}
