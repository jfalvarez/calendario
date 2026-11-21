package com.jfaf.calendario.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jfaf.calendario.data.sync.SyncEngine
import com.jfaf.calendario.di.appModules
import com.jfaf.calendario.domain.platform.ForegroundNotifier
import com.jfaf.calendario.presentation.App
import org.koin.core.context.startKoin
import java.awt.Frame
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File

fun main() {
    CrashReporting.init(loadAppVersion().let { "${it.versionName}+${it.versionCode}" }) // lo primero: recoge también los fallos al arrancar
    val koin = startKoin { modules(appModules + desktopModule()) }.koin
    val sync = koin.get<SyncEngine>()
    val foreground = koin.get<ForegroundNotifier>()
    // Se activa al iniciar sesión y se detiene al cerrarla.
    sync.start()
    val memory = WindowMemory(File(appDataDir(), "window.json"))
    application {
        val appIcon = remember { BitmapPainter(useResource("icon.png", ::loadImageBitmap)) }
        val saved = remember { memory.load()?.takeIf { it.isOnScreen() } }
        // Última geometría en modo ventana: si al cerrar está maximizada, es la que se recuerda para la posición y el tamaño.
        var lastNormal by remember { mutableStateOf(saved) }
        val windowState = rememberWindowState(
            size = saved?.let { DpSize(it.width.dp, it.height.dp) } ?: firstRunWindowSize(),
            position = saved?.let { WindowPosition(it.x.dp, it.y.dp) } ?: WindowPosition.Aligned(Alignment.Center),
            placement = if (saved?.maximized == true) WindowPlacement.Maximized else WindowPlacement.Floating,
        )
        Window(
            onCloseRequest = {
                lastNormal?.let { memory.save(it.copy(maximized = windowState.placement == WindowPlacement.Maximized)) }
                exitApplication()
            },
            state = windowState,
            title = "NurseOClock",
            icon = appIcon,
        ) {
            // Registra la geometría en modo ventana (en dp) para recordarla al cerrar.
            DisposableEffect(window) {
                fun recordNormal() {
                    if (window.extendedState != Frame.NORMAL) return
                    val scale = window.graphicsConfiguration.defaultTransform.scaleX.toFloat()
                    lastNormal = SavedWindow(
                        x = window.x / scale, y = window.y / scale,
                        width = window.width / scale, height = window.height / scale,
                        maximized = false,
                    )
                }
                recordNormal()
                val moves = object : ComponentAdapter() {
                    override fun componentMoved(e: ComponentEvent?) = recordNormal()
                    override fun componentResized(e: ComponentEvent?) = recordNormal()
                }
                window.addComponentListener(moves)
                // Al recuperar el foco o restaurar la ventana: sincroniza si hace tiempo que no se hacía.
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
                    window.removeComponentListener(moves)
                    window.removeWindowFocusListener(listener)
                    window.removeWindowListener(listener)
                }
            }
            App(backupIo = remember { DesktopBackupFileIo(window) })
        }
    }
}
