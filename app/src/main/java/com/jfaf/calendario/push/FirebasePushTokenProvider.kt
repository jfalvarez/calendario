package com.jfaf.calendario.push

import com.google.firebase.messaging.FirebaseMessaging
import com.jfaf.calendario.domain.repository.PushTokenProvider
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Token de Firebase Cloud Messaging de este dispositivo. Null si no se puede obtener (sin red o sin Play Services). */
class FirebasePushTokenProvider : PushTokenProvider {
    override suspend fun currentToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resume(null) }
    }
}
