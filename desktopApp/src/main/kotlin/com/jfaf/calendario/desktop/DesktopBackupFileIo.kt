package com.jfaf.calendario.desktop

import com.jfaf.calendario.presentation.backup.BackupFileIo
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/** Selector nativo de guardar y abrir de escritorio: el usuario elige la carpeta. */
class DesktopBackupFileIo(private val owner: Frame?) : BackupFileIo {

    override fun save(suggestedName: String, mimeType: String, content: String, onDone: (Boolean) -> Unit) {
        val dialog = FileDialog(owner, "Guardar copia", FileDialog.SAVE).apply {
            file = suggestedName
            isVisible = true
        }
        val name = dialog.file
        val folder = dialog.directory
        val ok = name != null && folder != null && runCatching {
            File(folder, name).writeText(content, Charsets.UTF_8)
        }.isSuccess
        onDone(ok)
    }

    override fun open(onPicked: (String?) -> Unit) {
        val dialog = FileDialog(owner, "Abrir copia", FileDialog.LOAD).apply { isVisible = true }
        val name = dialog.file
        val folder = dialog.directory
        onPicked(if (name != null && folder != null) File(folder, name).readText(Charsets.UTF_8) else null)
    }
}
