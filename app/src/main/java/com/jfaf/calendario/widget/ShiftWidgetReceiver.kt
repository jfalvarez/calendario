package com.jfaf.calendario.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class ShiftWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShiftWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // A medianoche (o al cambiar la hora/zona/arrancar) "mañana" pasa a ser "hoy": se vuelve a dibujar y se
        // programa la siguiente medianoche.
        if (intent.action in REFRESH_ACTIONS) {
            GlobalContext.get().get<ShiftWidgetUpdater>().start() // la ventana hoy-mañana se desplaza un día
            ShiftWidgetMidnight.schedule(context)
            val pending: BroadcastReceiver.PendingResult = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    glanceAppWidget.updateAll(context)
                } finally {
                    pending.finish()
                }
            }
        }
    }

    private companion object {
        val REFRESH_ACTIONS = setOf(
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            ShiftWidgetMidnight.ACTION_MIDNIGHT,
        )
    }
}
