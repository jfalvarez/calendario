package com.jfaf.calendario

import android.app.Application
import com.jfaf.calendario.data.sync.SyncEngine
import com.jfaf.calendario.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CalendarioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koin = startKoin {
            androidContext(this@CalendarioApplication)
            modules(appModules + androidModule)
        }.koin
        // Se activa al iniciar sesión y se detiene al cerrarla.
        koin.get<SyncEngine>().start()
    }
}
