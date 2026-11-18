package com.jfaf.calendario

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.jfaf.calendario.domain.model.AuthState
import com.jfaf.calendario.domain.usecase.ObserveAuthStateUseCase
import com.jfaf.calendario.domain.usecase.PushRegistration
import com.jfaf.calendario.presentation.App
import com.jfaf.calendario.reminder.AndroidReminderSystem
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val updateLauncher: PlayUpdateLauncher by inject()
    private val reminderSystem: AndroidReminderSystem by inject()
    private val observeAuth: ObserveAuthStateUseCase by inject()
    private val pushRegistration: PushRegistration by inject()
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val backupIo = com.jfaf.calendario.backup.AndroidBackupFileIo(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // antes de super.onCreate: pasa de Theme.Calendario.Starting a Theme.Calendario
        super.onCreate(savedInstanceState)
        updateLauncher.attach(this)
        reminderSystem.attach(this)
        // Avisos push (Android 13+): pedir permiso y asociar el token de este móvil cada vez que hay sesión.
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        lifecycleScope.launch {
            observeAuth().filterIsInstance<AuthState.SignedIn>().collect { pushRegistration.register() }
        }
        setContent {
            App(backupIo = backupIo, onDarkThemeChanged = { dark ->
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
