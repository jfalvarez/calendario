package com.jfaf.calendario.reminder

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jfaf.calendario.AndroidServiceLocator
import com.jfaf.calendario.MainActivity
import com.jfaf.calendario.R
import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.usecase.GetShiftsForDayUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.koin.core.qualifier.named

/** Suena la alarma de [ReminderScheduler]: muestra el turno del día siguiente y programa el siguiente aviso. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return
        val shiftDate = intent.getStringExtra(EXTRA_SHIFT_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val triggerAt = intent.getStringExtra(EXTRA_TRIGGER_AT)?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        val pending = goAsync()
        AndroidServiceLocator.get<CoroutineScope>(named("appScope")).launch {
            try {
                if (shiftDate != null) {
                    // Se lee el turno en este momento (no al programar): si cambió entre tanto, el aviso es el correcto.
                    val working = AndroidServiceLocator.get<GetShiftsForDayUseCase>()(shiftDate)
                    if (working.isNotEmpty()) show(context, working)
                }
                AndroidServiceLocator.get<ReminderScheduler>().reschedule(after = triggerAt)
            } finally {
                pending.finish()
            }
        }
    }

    @SuppressLint("MissingPermission") // se comprueba con areNotificationsEnabled()
    private fun show(context: Context, shifts: List<Shift>) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.reminder_channel_description) },
        )
        val lines = shifts.map { shift ->
            val hours = shift.hours()
            if (hours != null) context.getString(R.string.reminder_line, shift.name, hours) else shift.name
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun Shift.hours(): String? {
        val start = startMinutes ?: return null
        val end = endMinutes ?: return null
        fun clock(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)
        return "${clock(start)}–${clock(end)}"
    }

    companion object {
        const val ACTION_REMIND = "com.jfaf.calendario.action.REMIND"
        const val EXTRA_SHIFT_DATE = "shift_date"
        const val EXTRA_TRIGGER_AT = "trigger_at"
        private const val CHANNEL_ID = "shift_reminders"
        private const val NOTIFICATION_ID = 4101
    }
}
