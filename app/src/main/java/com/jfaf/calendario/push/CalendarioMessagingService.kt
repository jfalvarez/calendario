package com.jfaf.calendario.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.jfaf.calendario.domain.usecase.PushRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Firebase cambia el token de vez en cuando; aquí se registra el nuevo si hay sesión.
 * Los avisos con texto los muestra el propio sistema cuando la app está en segundo plano.
 */
class CalendarioMessagingService : FirebaseMessagingService(), KoinComponent {
    private val pushRegistration: PushRegistration by inject()

    override fun onNewToken(token: String) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { pushRegistration.register(token) }
    }
}
