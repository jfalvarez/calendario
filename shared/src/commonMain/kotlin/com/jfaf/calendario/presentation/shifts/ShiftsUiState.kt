package com.jfaf.calendario.presentation.shifts

import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.model.ShiftValidationError

/** Errores del formulario: los del dominio más los de formato de texto (sólo existen en la UI). */
enum class ShiftEditorError {
    NameRequired, AbbreviationRequired, AbbreviationTooLong, TimesIncomplete, BreakInvalid,
    /** Hora que no es `HH:mm`. */
    TimeFormat,
    /** Descanso que no es un número. */
    BreakFormat,
}

internal fun ShiftValidationError.toEditorError() = when (this) {
    ShiftValidationError.NameRequired -> ShiftEditorError.NameRequired
    ShiftValidationError.AbbreviationRequired -> ShiftEditorError.AbbreviationRequired
    ShiftValidationError.AbbreviationTooLong -> ShiftEditorError.AbbreviationTooLong
    ShiftValidationError.TimesIncomplete -> ShiftEditorError.TimesIncomplete
    ShiftValidationError.BreakInvalid -> ShiftEditorError.BreakInvalid
}

/** Formulario de alta/edición; los campos de texto se mantienen tal como los escribe el usuario. */
data class ShiftEditorState(
    /** Vacío = turno nuevo. */
    val id: String = "",
    val name: String = "",
    val abbreviation: String = "",
    val colorArgb: Long = ShiftColors.first(),
    val start: String = "",
    val end: String = "",
    val breakMinutes: String = "",
    val error: ShiftEditorError? = null,
) {
    val isNew: Boolean get() = id.isEmpty()
}

data class ShiftsUiState(
    val shifts: List<Shift> = emptyList(),
    val editor: ShiftEditorState? = null,
    /** Turno pendiente de confirmar su eliminación. */
    val pendingDelete: Shift? = null,
)

/**
 * Paleta de colores ofrecida al crear un turno. Cada fila de 6 es: colores base y debajo su versión más clara
 * (mezcla al 50 % con blanco), así el selector queda en dos filas por tanda de seis tonos.
 */
val ShiftColors: List<Long> = listOf(
    0xFF2196F3, 0xFF03A9F4, 0xFF009688, 0xFF4CAF50, 0xFF8BC34A, 0xFFFFC107,
    0xFF90CAF9, 0xFF81D4F9, 0xFF7FCAC3, 0xFFA5D7A7, 0xFFC5E1A4, 0xFFFFE083,
    0xFFFF9800, 0xFFFF5722, 0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF3F51B5,
    0xFFFFCB7F, 0xFFFFAB90, 0xFFF9A19A, 0xFFF48EB1, 0xFFCD93D7, 0xFF9FA8DA,
)
