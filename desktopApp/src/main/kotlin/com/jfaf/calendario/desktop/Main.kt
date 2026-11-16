package com.jfaf.calendario.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jfaf.calendario.di.appModules
import com.jfaf.calendario.presentation.App
import org.koin.core.context.startKoin

fun main() {
    startKoin { modules(appModules + desktopModule()) }
    application {
        Window(onCloseRequest = ::exitApplication, title = "Calendario") {
            App()
        }
    }
}
