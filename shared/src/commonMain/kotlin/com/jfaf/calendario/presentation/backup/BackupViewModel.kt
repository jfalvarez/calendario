package com.jfaf.calendario.presentation.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.repository.TodayProvider
import com.jfaf.calendario.domain.usecase.ExportBackupUseCase
import com.jfaf.calendario.domain.usecase.ImportBackupUseCase
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Resultado de la última acción de copia, para mostrarlo en Ajustes. */
enum class BackupNotice { Exported, Imported, InvalidFile, Failed }

data class BackupUiState(
    val busy: Boolean = false,
    val notice: BackupNotice? = null,
    /** Texto de una copia elegida que espera la confirmación de sustitución. */
    val pendingImport: String? = null,
)

@OptIn(ExperimentalTime::class)
class BackupViewModel(
    private val export: ExportBackupUseCase,
    private val import: ImportBackupUseCase,
    private val today: TodayProvider,
    private val clock: Clock,
) : ViewModel() {

    private val state = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = state.asStateFlow()

    /** Guarda la copia completa (JSON) y, después, las dos hojas en CSV para leer en Excel. */
    fun exportAll(io: BackupFileIo) {
        if (state.value.busy) return
        state.update { it.copy(busy = true, notice = null) }
        viewModelScope.launch {
            val files = export(clock.now().toString())
            val stamp = today.today().toString()
            io.save("calendario-$stamp.json", "application/json", files.json) { saved ->
                if (!saved) return@save finish(BackupNotice.Failed)
                io.save("turnos-$stamp.csv", "text/csv", files.shiftsCsv) { _ ->
                    io.save("notas-$stamp.csv", "text/csv", files.notesCsv) { _ -> finish(BackupNotice.Exported) }
                }
            }
        }
    }

    /** Abre una copia y la deja pendiente de confirmación. No toca nada hasta que el usuario confirma. */
    fun pickImport(io: BackupFileIo) {
        io.open { text ->
            if (text == null) return@open
            state.update { it.copy(pendingImport = text, notice = null) }
        }
    }

    fun cancelImport() = state.update { it.copy(pendingImport = null) }

    /** Sustituye todos los datos por los de la copia confirmada. */
    fun confirmImport() {
        val text = state.value.pendingImport ?: return
        state.update { it.copy(busy = true, pendingImport = null, notice = null) }
        viewModelScope.launch {
            val result = import(text)
            finish(
                when {
                    result.isSuccess -> BackupNotice.Imported
                    result.exceptionOrNull() is com.jfaf.calendario.domain.backup.BackupFormatException -> BackupNotice.InvalidFile
                    else -> BackupNotice.Failed
                },
            )
        }
    }

    private fun finish(notice: BackupNotice) = state.update { it.copy(busy = false, notice = notice) }
}
