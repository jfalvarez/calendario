package com.jfaf.calendario.domain.model

enum class ShiftValidationError {
    NameRequired,
    AbbreviationRequired,
    AbbreviationTooLong,
    /** Hay hora de inicio sin hora de fin (o al revés). */
    TimesIncomplete,
    /** Descanso negativo o igual/mayor que la jornada. */
    BreakInvalid,
}
