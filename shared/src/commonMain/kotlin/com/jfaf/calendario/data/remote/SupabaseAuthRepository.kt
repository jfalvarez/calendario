package com.jfaf.calendario.data.remote

import com.jfaf.calendario.domain.model.AuthError
import com.jfaf.calendario.domain.model.AuthState
import com.jfaf.calendario.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SupabaseAuthRepository(private val client: SupabaseClient) : AuthRepository {

    override val authState: Flow<AuthState> = client.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> status.session.toState()
            is SessionStatus.Initializing -> AuthState.Loading
            is SessionStatus.NotAuthenticated -> AuthState.SignedOut
            // Sin conexión al arrancar el refresco de la sesión falla, pero la sesión guardada sigue ahí:
            // se mantiene la cuenta iniciada y la sincronización reintenta cuando haya red.
            else -> client.auth.currentSessionOrNull()?.toState() ?: AuthState.SignedOut
        }
    }

    private fun io.github.jan.supabase.auth.user.UserSession.toState(): AuthState =
        user?.id?.let { AuthState.SignedIn(it, user?.email) } ?: AuthState.SignedOut

    override suspend fun signIn(email: String, password: String): AuthError? = call {
        client.auth.signInWith(Email) { this.email = email; this.password = password }
    }

    override suspend fun signUp(email: String, password: String): AuthError? =
        call { client.auth.signUpWith(Email) { this.email = email; this.password = password } }
            ?: if (client.auth.currentSessionOrNull() == null) AuthError.EmailConfirmationRequired else null

    override suspend fun signOut() {
        try {
            client.auth.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Sin conexión: cierra al menos la sesión local.
            client.auth.clearSession()
        }
    }

    private suspend fun call(block: suspend () -> Unit): AuthError? = try {
        block()
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: RestException) {
        mapRestError(e.error, e.description)
    } catch (e: Exception) {
        if (looksOffline(e)) AuthError.Network else AuthError.Unknown
    }
}

/** Traduce el error de Supabase Auth a un error de dominio (por el código o, si no, por el texto). */
internal fun mapRestError(error: String, description: String?): AuthError {
    val text = "$error ${description.orEmpty()}".lowercase()
    return when {
        "invalid_credentials" in text || "invalid login credentials" in text -> AuthError.InvalidCredentials
        "already" in text && ("registered" in text || "exists" in text) -> AuthError.EmailAlreadyUsed
        "weak_password" in text || "password should be" in text -> AuthError.WeakPassword
        "email_address_invalid" in text || "unable to validate email" in text || "invalid email" in text -> AuthError.InvalidEmail
        else -> AuthError.Unknown
    }
}

/** Heurística multiplataforma: ¿el fallo es de conexión? */
internal fun looksOffline(e: Throwable): Boolean {
    var current: Throwable? = e
    while (current != null) {
        val name = current::class.simpleName.orEmpty()
        if (listOf("UnknownHost", "UnresolvedAddress", "ConnectException", "SocketTimeout", "ConnectTimeout",
                "HttpRequestTimeout", "NoRouteToHost", "SSLException", "IOException", "Network").any { name.contains(it) }
        ) return true
        current = current.cause
    }
    return false
}
