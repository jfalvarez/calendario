package com.jfaf.calendario.data.local

import com.jfaf.calendario.domain.model.Shift

internal fun ShiftEntity.toDomain() =
    Shift(id, name, abbreviation, colorArgb, startMinutes, endMinutes, breakMinutes)
