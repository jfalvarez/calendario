package com.jfaf.calendario.fakes

import com.jfaf.calendario.domain.repository.LocalHolidayRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

class InMemoryLocalHolidayRepository : LocalHolidayRepository {
    private val data = MutableStateFlow<Set<LocalDate>>(emptySet())

    override fun observeHolidays(from: LocalDate, to: LocalDate): Flow<Set<LocalDate>> =
        data.map { all -> all.filter { it in from..to }.toSet() }

    override suspend fun getHolidays(dates: Set<LocalDate>): Set<LocalDate> = data.value.intersect(dates)

    override suspend fun setHoliday(dates: Set<LocalDate>, holiday: Boolean) =
        data.update { if (holiday) it + dates else it - dates }
}
