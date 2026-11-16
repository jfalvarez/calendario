package com.jfaf.calendario.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jfaf.calendario.data.sync.SyncEngine
import com.jfaf.calendario.di.appModules
import com.jfaf.calendario.presentation.App
import org.koin.core.context.startKoin
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

fun main() {
    val koin = startKoin { modules(appModules + desktopModule()) }.koin
    val sync = koin.get<SyncEngine>()
    // Se activa al iniciar sesión y se detiene al cerrarla.
    sync.start()
    application {
        Window(onCloseRequest = ::exitApplication, title = "Calendario") {
            // Al recuperar el foco o restaurar la ventana: sincroniza si hace tiempo que no se hacía.
            DisposableEffect(window) {
                val listener = object : WindowAdapter() {
                    override fun windowGainedFocus(e: WindowEvent?) = sync.requestSyncIfStale()
                    override fun windowDeiconified(e: WindowEvent?) = sync.requestSyncIfStale()
                }
                window.addWindowFocusListener(listener)
                window.addWindowListener(listener)
                onDispose {
                    window.removeWindowFocusListener(listener)
                    window.removeWindowListener(listener)
                }
            }
            App()
        }
    }
}
