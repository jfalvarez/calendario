package com.jfaf.calendario.domain.usecase

import com.jfaf.calendario.domain.repository.LocalHolidayRepository
import kotlinx.datetime.LocalDate

/** Si todos los días ya eran festivo local, los desmarca; si no, marca todos. */
class ToggleLocalHolidaysUseCase(private val holidays: LocalHolidayRepository) {
    suspend operator fun invoke(dates: Set<LocalDate>) {
        if (dates.isEmpty()) return
        holidays.setHoliday(dates, holiday = holidays.getHolidays(dates).size != dates.size)
    }
}
