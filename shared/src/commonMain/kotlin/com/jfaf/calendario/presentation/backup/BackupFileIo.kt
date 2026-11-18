package com.jfaf.calendario.presentation.backup

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Guardar y abrir ficheros con el selector de cada plataforma. El usuario elige la carpeta o la app de destino;
 * la app no envía nada a ningún servidor ni correo.
 */
interface BackupFileIo {
    /** Guarda [content] en el sitio que elija el usuario. [onDone] recibe `true` si se guardó. */
    fun save(suggestedName: String, mimeType: String, content: String, onDone: (Boolean) -> Unit)

    /** Pide un fichero para abrir. [onPicked] recibe su texto, o `null` si el usuario cancela. */
    fun open(onPicked: (String?) -> Unit)
}

/** Lo aporta cada plataforma. Null donde no hay selector de ficheros (la sección de copia se oculta). */
val LocalBackupFileIo = staticCompositionLocalOf<BackupFileIo?> { null }
