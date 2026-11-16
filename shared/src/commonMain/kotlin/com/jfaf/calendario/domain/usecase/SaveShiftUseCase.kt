package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.model.Shift
import com.jfaf.calendario.domain.model.ShiftValidationError
import com.jfaf.calendario.domain.repository.IdGenerator
import com.jfaf.calendario.domain.repository.ShiftRepository

/** Valida y guarda un turno. Con `shift.id` vacío se crea uno nuevo con id generado. */
class SaveShiftUseCase(
    private val repository: ShiftRepository,
    private val idGenerator: IdGenerator,
) {
    /** Devuelve el error de validación, o `null` si se guardó. */
    suspend operator fun invoke(shift: Shift): ShiftValidationError? {
        val clean = shift.copy(name = shift.name.trim(), abbreviation = shift.abbreviation.trim())
        validate(clean)?.let { return it }
        repository.upsert(if (clean.id.isBlank()) clean.copy(id = idGenerator.newId()) else clean)
        return null
    }

    private fun validate(shift: Shift): ShiftValidationError? = when {
        shift.name.isEmpty() -> ShiftValidationError.NameRequired
        shift.abbreviation.isEmpty() -> ShiftValidationError.AbbreviationRequired
        shift.abbreviation.length > Shift.MAX_ABBREVIATION_LENGTH -> ShiftValidationError.AbbreviationTooLong
        (shift.startMinutes == null) != (shift.endMinutes == null) -> ShiftValidationError.TimesIncomplete
        shift.breakMinutes < 0 -> ShiftValidationError.BreakInvalid
        shift.startMinutes != null && shift.endMinutes != null && shift.breakMinutes > 0 &&
            shift.breakMinutes >= shiftSpan(shift) -> ShiftValidationError.BreakInvalid
        else -> null
    }

    private fun shiftSpan(shift: Shift): Int {
        val raw = ((shift.endMinutes!! - shift.startMinutes!!) % Shift.MINUTES_PER_DAY + Shift.MINUTES_PER_DAY) % Shift.MINUTES_PER_DAY
        return if (raw == 0) Shift.MINUTES_PER_DAY else raw
    }
}
