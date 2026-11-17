package com.jfaf.calendario

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.jfaf.calendario.data.sync.SyncEngine
import com.jfaf.calendario.di.appModules
import com.jfaf.calendario.presentation.update.AppForegroundEvents
import com.jfaf.calendario.widget.ShiftWidgetUpdater
import com.jfaf.calendario.crash.CrashReporting
import com.jfaf.calendario.reminder.ReminderUpdater
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CalendarioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporting.init(this) // lo primero: recoge también los fallos al arrancar Koin
        val koin = startKoin {
            androidContext(this@CalendarioApplication)
            modules(appModules + androidModule)
        }.koin
        val sync = koin.get<SyncEngine>()
        val foreground = koin.get<AppForegroundEvents>()
        koin.get<ShiftWidgetUpdater>().start()
        val reminders = koin.get<ReminderUpdater>()
        reminders.start()
        // Se activa al iniciar sesión y se detiene al cerrarla.
        sync.start()
        // Cada vez que la app pasa a primer plano (no a cada pantalla): sincroniza si hace tiempo que no se hacía.
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                sync.requestSyncIfStale()
                reminders.start() // reinicia la ventana de fechas observada (parte de hoy)
                foreground.notifyForeground()
            }
        })
    }
}
