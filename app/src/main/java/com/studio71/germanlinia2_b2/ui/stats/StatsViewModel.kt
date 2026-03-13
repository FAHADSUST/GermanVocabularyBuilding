package com.studio71.germanlinia2_b2.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studio71.germanlinia2_b2.data.local.DailyStatEntity
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class StatRange(val label: String, val days: Int) {
    WEEK("Woche", 7),
    MONTH("Monat", 30),
    YEAR("Jahr", 365)
}

data class StatBar(
    val label: String,
    val learned: Int,
    val reviewed: Int
)

data class StatsUiState(
    val bars: List<StatBar> = emptyList(),
    val totalLearned: Int = 0,
    val currentStreak: Int = 0,
    val range: StatRange = StatRange.WEEK
)

class StatsViewModel(
    private val repo: VocabularyRepository
) : ViewModel() {

    private val _range = MutableStateFlow(StatRange.WEEK)
    val range: StateFlow<StatRange> = _range.asStateFlow()

    val state: StateFlow<StatsUiState> =
        combine(repo.observeAllStats(), _range) { stats, range ->
            buildState(stats, range)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun setRange(range: StatRange) { _range.value = range }

    private fun buildState(stats: List<DailyStatEntity>, range: StatRange): StatsUiState {
        val byDate = stats.associateBy { it.date }
        val today = LocalDate.now()
        val bars = (range.days - 1 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            val stat = byDate[date.toEpochDay()]
            StatBar(
                label = "${date.dayOfMonth}.${date.monthValue}",
                learned = stat?.wordsLearned ?: 0,
                reviewed = stat?.wordsReviewed ?: 0
            )
        }
        return StatsUiState(
            bars = bars,
            totalLearned = stats.sumOf { it.wordsLearned },
            currentStreak = computeStreak(byDate.keys),
            range = range
        )
    }

    /** Counts consecutive days (ending today or yesterday) that have activity. */
    private fun computeStreak(activeDays: Set<Long>): Int {
        var streak = 0
        var day = LocalDate.now().toEpochDay()
        if (day !in activeDays) day -= 1 // allow today to be empty so far
        while (day in activeDays) {
            streak++
            day -= 1
        }
        return streak
    }

    class Factory(private val repo: VocabularyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StatsViewModel(repo) as T
    }
}

