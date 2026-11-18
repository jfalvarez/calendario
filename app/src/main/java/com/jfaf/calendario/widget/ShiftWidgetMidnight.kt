package com.jfaf.calendario.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.ZonedDateTime

/**
 * Programa un aviso para justo después de la medianoche local. Sin él, el widget sólo se redibuja si cambian datos o
 * si el sistema entrega DATE_CHANGED, y en ese caso seguiría mostrando el día anterior como "Hoy".
 * La alarma es aproximada (no necesita permiso de alarmas exactas): basta con que llegue de madrugada.
 */
object ShiftWidgetMidnight {
    const val ACTION_MIDNIGHT = "com.jfaf.calendario.widget.MIDNIGHT"
    private const val REQUEST_CODE = 4102

    fun schedule(context: Context) {
        val now = ZonedDateTime.now()
        val next = now.toLocalDate().plusDays(1).atStartOfDay(now.zone).plusMinutes(1)
        val intent = Intent(context, ShiftWidgetReceiver::class.java).setAction(ACTION_MIDNIGHT)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), pendingIntent)
    }
}
