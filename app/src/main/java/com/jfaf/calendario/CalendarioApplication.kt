package com.jfaf.calendario

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.jfaf.calendario.data.sync.SyncEngine
import com.jfaf.calendario.di.appModules
import com.jfaf.calendario.presentation.update.AppForegroundEvents
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CalendarioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koin = startKoin {
            androidContext(this@CalendarioApplication)
            modules(appModules + androidModule)
        }.koin
        val sync = koin.get<SyncEngine>()
        val foreground = koin.get<AppForegroundEvents>()
        // Se activa al iniciar sesión y se detiene al cerrarla.
        sync.start()
        // Cada vez que la app pasa a primer plano (no a cada pantalla): sincroniza si hace tiempo que no se hacía.
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                sync.requestSyncIfStale()
                foreground.notifyForeground()
            }
        })
    }
}
