package com.jfaf.calendario.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.io.File
import java.util.Properties

/**
 * Geometría de la ventana al cerrar, en dp de Compose (las mismas unidades que `WindowState`).
 * `maximized` indica que estaba maximizada; `x`/`y`/`width`/`height` son su tamaño y posición en modo ventana.
 */
data class SavedWindow(val x: Float, val y: Float, val width: Float, val height: Float, val maximized: Boolean)

/** Recuerda la ventana entre ejecuciones en un fichero de propiedades de la carpeta de datos del usuario. */
class WindowMemory(private val file: File) {
    fun load(): SavedWindow? = runCatching {
        if (!file.exists()) return null
        val p = Properties().apply { file.reader().use { load(it) } }
        SavedWindow(
            x = p.getProperty("x").toFloat(),
            y = p.getProperty("y").toFloat(),
            width = p.getProperty("width").toFloat(),
            height = p.getProperty("height").toFloat(),
            maximized = p.getProperty("maximized").toBoolean(),
        )
    }.getOrNull()

    fun save(window: SavedWindow) {
        runCatching {
            file.parentFile?.mkdirs()
            val p = Properties().apply {
                setProperty("x", window.x.toString())
                setProperty("y", window.y.toString())
                setProperty("width", window.width.toString())
                setProperty("height", window.height.toString())
                setProperty("maximized", window.maximized.toString())
            }
            file.writer().use { p.store(it, "Ventana de NurseOClock") }
        }
    }
}

/**
 * ¿Se ve al menos una franja de la ventana guardada en alguna pantalla actual? Si se desconectó el monitor en el que
 * estaba, no se restaura y la ventana sale en su posición por defecto.
 */
fun SavedWindow.isOnScreen(): Boolean {
    val env = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val screens = env.screenDevices.map { it.defaultConfiguration }
    if (screens.isEmpty()) return false
    val scale = screens.first().defaultTransform.scaleX.toFloat()
    val window = Rectangle(
        (x * scale).toInt(), (y * scale).toInt(), (width * scale).toInt(), (height * scale).toInt(),
    )
    val minVisible = 100
    return screens.any { config ->
        val overlap = window.intersection(config.bounds)
        overlap.width >= minVisible && overlap.height >= minVisible
    }
}

/**
 * Tamaño de la primera vez (sin datos guardados): la mitad de la pantalla principal, en dp de Compose.
 * La posición se centra desde `WindowPosition.Aligned` en `Main.kt`.
 */
fun firstRunWindowSize(): DpSize {
    val config = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
    val scale = config.defaultTransform.scaleX.toFloat()
    val bounds = config.bounds
    return DpSize((bounds.width / scale / 2).dp, (bounds.height / scale / 2).dp)
}
