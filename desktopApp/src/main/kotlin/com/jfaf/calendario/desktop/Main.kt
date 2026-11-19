package com.jfaf.calendario.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.jfaf.calendario.data.sync.SyncEngine
import com.jfaf.calendario.di.appModules
import com.jfaf.calendario.presentation.App
import com.jfaf.calendario.domain.platform.ForegroundNotifier
import org.koin.core.context.startKoin
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

fun main() {
    CrashReporting.init(loadAppVersion().let { "${it.versionName}+${it.versionCode}" }) // lo primero: recoge también los fallos al arrancar
    val koin = startKoin { modules(appModules + desktopModule()) }.koin
    val sync = koin.get<SyncEngine>()
    val foreground = koin.get<ForegroundNotifier>()
    // Se activa al iniciar sesión y se detiene al cerrarla.
    sync.start()
    application {
        val appIcon = remember { BitmapPainter(useResource("icon.png", ::loadImageBitmap)) }
        Window(onCloseRequest = ::exitApplication, title = "NurseOClock", icon = appIcon) {
            // Al recuperar el foco o restaurar la ventana: sincroniza si hace tiempo que no se hacía.
            DisposableEffect(window) {
                val listener = object : WindowAdapter() {
                    override fun windowGainedFocus(e: WindowEvent?) = onForeground()
                    override fun windowDeiconified(e: WindowEvent?) = onForeground()

                    private fun onForeground() {
                        sync.requestSyncIfStale()
                        foreground.notifyForeground()
                    }
                }
                window.addWindowFocusListener(listener)
                window.addWindowListener(listener)
                onDispose {
                    window.removeWindowFocusListener(listener)
                    window.removeWindowListener(listener)
                }
            }
            App(backupIo = remember { DesktopBackupFileIo(window) })
        }
    }
}
