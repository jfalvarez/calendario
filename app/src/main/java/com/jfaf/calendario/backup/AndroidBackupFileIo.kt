package com.jfaf.calendario.backup

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.jfaf.calendario.domain.platform.BackupFileIo

/**
 * Selector de guardar y abrir de Android (el del sistema: el usuario elige carpeta o app).
 * Se crea en la actividad, porque los selectores deben registrarse antes de que la actividad arranque.
 */
class AndroidBackupFileIo(private val activity: ComponentActivity) : BackupFileIo {
    private var content: String? = null
    private var onSaved: ((Boolean) -> Unit)? = null
    private var onOpened: ((String?) -> Unit)? = null

    private val createJson = activity.registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { finishSave(it) }
    private val createCsv = activity.registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { finishSave(it) }
    private val openDocument = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val text = uri?.let { activity.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() } }
        onOpened?.invoke(text)
        onOpened = null
    }

    override fun save(suggestedName: String, mimeType: String, content: String, onDone: (Boolean) -> Unit) {
        this.content = content
        onSaved = onDone
        if (mimeType == "application/json") createJson.launch(suggestedName) else createCsv.launch(suggestedName)
    }

    override fun open(onPicked: (String?) -> Unit) {
        onOpened = onPicked
        openDocument.launch(arrayOf("*/*"))
    }

    private fun finishSave(uri: android.net.Uri?) {
        val text = content
        // Se guarda el callback antes de llamarlo: el siguiente guardado (p. ej. el segundo CSV) lo reasigna
        // desde dentro, y no debemos borrarlo después.
        val done = onSaved
        content = null
        onSaved = null
        val ok = uri != null && text != null && runCatching {
            activity.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }.isSuccess
        done?.invoke(ok)
    }
}
