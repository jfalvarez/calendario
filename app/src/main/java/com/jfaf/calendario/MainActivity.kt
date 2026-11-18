package com.jfaf.calendario

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jfaf.calendario.presentation.App
import com.jfaf.calendario.reminder.AndroidReminderSystem
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val updateLauncher: PlayUpdateLauncher by inject()
    private val reminderSystem: AndroidReminderSystem by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // antes de super.onCreate: pasa de Theme.Calendario.Starting a Theme.Calendario
        super.onCreate(savedInstanceState)
        updateLauncher.attach(this)
        reminderSystem.attach(this)
        setContent {
            App(onDarkThemeChanged = { dark ->
                // Iconos de las barras del sistema legibles según el tema elegido en la app (no sólo el del sistema).
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            })
        }
    }

    override fun onResume() {
        super.onResume()
        updateLauncher.resumeIfInProgress()
    }

    override fun onDestroy() {
        updateLauncher.detach(this)
        super.onDestroy()
    }
}
