package com.jfaf.calendario.reminder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jfaf.calendario.domain.platform.ReminderSystem

/** Permiso de notificaciones de Android (obligatorio pedirlo en Android 13+). Se enlaza a la actividad al crearse. */
class AndroidReminderSystem(private val context: Context) : ReminderSystem {
    private var launcher: ActivityResultLauncher<String>? = null
    private var pending: ((Boolean) -> Unit)? = null

    override val supported = true

    /** Debe llamarse en `onCreate`, antes de que la actividad pase a STARTED. */
    fun attach(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pending?.invoke(granted)
            pending = null
        }
    }

    override fun notificationsAllowed(): Boolean {
        val permissionOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionOk && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    override fun requestNotifications(onResult: (granted: Boolean) -> Unit) {
        val needsPrompt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        val activityLauncher = launcher
        if (!needsPrompt || activityLauncher == null) {
            onResult(notificationsAllowed())
            return
        }
        pending = onResult
        activityLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
