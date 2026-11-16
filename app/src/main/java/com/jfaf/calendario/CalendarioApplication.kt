package com.jfaf.calendario

import android.app.Application
import com.jfaf.calendario.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CalendarioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CalendarioApplication)
            modules(appModules)
        }
    }
}
