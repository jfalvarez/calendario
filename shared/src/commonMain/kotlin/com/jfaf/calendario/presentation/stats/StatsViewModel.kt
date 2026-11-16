package com.jfaf.calendario.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfaf.calendario.domain.model.PeriodStats
import com.jfaf.calendario.domain.model.StatsPeriod
import com.jfaf.calendario.domain.repository.TodayProvider
import com.jfaf.calendario.domain.usecase.ObserveStatsUseCase
import com.jfaf.calendario.domain.usecase.resolve
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

data class StatsUiState(
    val period: StatsPeriod,
    val from: LocalDate,
    val to: LocalDate,
    /** `null` mientras se calcula. */
    val stats: PeriodStats? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    observeStats: ObserveStatsUseCase,
    todayProvider: TodayProvider,
) : ViewModel() {
    private data class Selection(val period: StatsPeriod, val from: LocalDate, val to: LocalDate)

    private val today = todayProvider.today()
    private val selection = MutableStateFlow(StatsPeriod.ThisMonth.resolve(today)!!.let {
        Selection(StatsPeriod.ThisMonth, it.start, it.endInclusive)
    })

    val uiState: StateFlow<StatsUiState> = selection.flatMapLatest { sel ->
        observeStats(sel.from, sel.to).map { StatsUiState(sel.period, sel.from, sel.to, it) }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        selection.value.let { StatsUiState(it.period, it.from, it.to) },
    )

    fun onPeriodSelected(period: StatsPeriod) {
        val range = period.resolve(today) ?: return // `Custom` entra por onCustomRange
        selection.value = Selection(period, range.start, range.endInclusive)
    }

    fun onCustomRange(from: LocalDate, to: LocalDate) {
        val (start, end) = if (from <= to) from to to else to to from
        selection.update { Selection(StatsPeriod.Custom, start, end) }
    }
}
