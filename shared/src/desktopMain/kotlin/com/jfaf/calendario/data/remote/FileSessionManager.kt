package com.jfaf.calendario.data.remote

import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions

/**
 * Guarda la sesión de Supabase (con el refresh token) en un archivo de la carpeta de datos del usuario, con permisos
 * sólo para su dueño (600 en Linux/macOS; en Windows `%APPDATA%` ya es privada del usuario), en lugar del registro
 * o las preferencias compartidas de Java (`java.util.prefs`), que son más fáciles de leer para otros procesos.
 * La escritura es atómica (archivo temporal + renombrado). Un archivo ilegible se trata como «sin sesión».
 *
 * `encodeDefaults` debe ser `true`: `UserSession.expiresAt` tiene valor por defecto y, si no se escribe, al cargar
 * la sesión la librería cree que el token vale otra hora, nunca lo renueva y falla con «JWT expired».
 * Una sesión sin `expiresAt` (guardada con la versión anterior) se descarta y hay que volver a iniciar sesión.
 */
class FileSessionManager(private val file: File) : SessionManager {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun saveSession(session: UserSession) {
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(UserSession.serializer(), session))
            restrictToOwner(tmp)
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    override suspend fun loadSession(): UserSession = withContext(Dispatchers.IO) {
        val text = file.readText()
        if ("expiresAt" !in json.parseToJsonElement(text).jsonObject) {
            file.delete()
            throw IllegalStateException("Sesión guardada sin fecha de caducidad: se descarta")
        }
        json.decodeFromString(UserSession.serializer(), text)
    }

    override suspend fun loadSessionOrNull(): UserSession? =
        if (!file.exists()) null else runCatching { loadSession() }.getOrNull()

    override suspend fun deleteSession() = withContext(Dispatchers.IO) {
        file.delete()
        Unit
    }

    private fun restrictToOwner(target: File) {
        runCatching { Files.setPosixFilePermissions(target.toPath(), PosixFilePermissions.fromString("rw-------")) }
    }
}
