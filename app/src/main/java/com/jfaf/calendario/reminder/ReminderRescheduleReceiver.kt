package com.jfaf.calendario.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/** Las alarmas se pierden al reiniciar y dependen de la hora y la zona: se vuelve a programar el aviso. */
class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                GlobalContext.get().get<ReminderScheduler>().reschedule()
            } finally {
                pending.finish()
            }
        }
    }
}
