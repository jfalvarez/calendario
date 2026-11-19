package com.jfaf.calendario.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.jfaf.calendario.domain.repository.TodayProvider
import com.jfaf.calendario.domain.usecase.ObserveUpcomingDaysUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Redibuja el widget cuando cambian los turnos de hoy o de mañana (pintar en la app, sincronizar, editar un turno).
 * Vive mientras viva el proceso; si éste muere, el widget conserva lo último dibujado hasta el siguiente cambio.
 */
class ShiftWidgetUpdater(
    private val context: Context,
    private val observeUpcoming: ObserveUpcomingDaysUseCase,
    private val today: TodayProvider,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null

    /** Empieza a observar hoy y mañana; si ya observaba, reinicia (al cambiar de día la ventana de fechas se desplaza). */
    fun start() {
        job?.cancel()
        job = scope.launch {
            observeUpcoming(today.today(), days = 2)
                .distinctUntilChanged()
                .drop(1) // la primera emisión es el estado actual, que el widget ya lee al dibujarse
                .collect { ShiftWidget().updateAll(context) }
        }
    }
}
