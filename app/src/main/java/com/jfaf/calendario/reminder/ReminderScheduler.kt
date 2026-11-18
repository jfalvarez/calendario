package com.jfaf.calendario.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jfaf.calendario.domain.usecase.PlanNextReminderUseCase
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.ZoneId

/**
 * Programa una única alarma: el siguiente aviso. Al sonar, [ReminderReceiver] muestra la notificación y vuelve a
 * programar el siguiente. Se llama al cambiar ajustes o turnos, tras reiniciar y al cambiar la hora o la zona.
 */
class ReminderScheduler(
    private val context: Context,
    private val plan: PlanNextReminderUseCase,
) {
    private val alarms get() = context.getSystemService(AlarmManager::class.java)

    /** [after] evita volver a programar el aviso que acaba de sonar si el reloj lo adelantó unos milisegundos. */
    suspend fun reschedule(after: LocalDateTime? = null) {
        val now = java.time.LocalDateTime.now().toKotlinLocalDateTime().let { if (after != null && after > it) after else it }
        val next = plan(now)
        val pendingIntent = pendingIntent(next?.shiftDate?.toString(), next?.triggerAt?.toString())
        if (next == null) {
            alarms.cancel(pendingIntent)
            return
        }
        val at = next.triggerAt.toJavaLocalDateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        try {
            if (exact) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
        } catch (_: SecurityException) {
            // El permiso de alarmas exactas se retiró entre la comprobación y la llamada: vale el aviso aproximado.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent)
        }
    }

    private fun pendingIntent(shiftDate: String?, triggerAt: String?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            // Los extras no cuentan para igualar PendingIntents: siempre hay una única alarma y se reemplaza.
            shiftDate?.let { putExtra(ReminderReceiver.EXTRA_SHIFT_DATE, it) }
            triggerAt?.let { putExtra(ReminderReceiver.EXTRA_TRIGGER_AT, it) }
        }
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        const val REQUEST_CODE = 4101
    }
}
