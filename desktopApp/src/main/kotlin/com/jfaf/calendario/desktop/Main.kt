package com.jfaf.calendario.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jfaf.calendario.data.sync.SyncEngine
import com.jfaf.calendario.di.appModules
import com.jfaf.calendario.presentation.App
import org.koin.core.context.startKoin

fun main() {
    val koin = startKoin { modules(appModules + desktopModule()) }.koin
    // Se activa al iniciar sesión y se detiene al cerrarla.
    koin.get<SyncEngine>().start()
    application {
        Window(onCloseRequest = ::exitApplication, title = "Calendario") {
            App()
        }
    }
}
