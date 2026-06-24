package com.studio71.germanlinia2_b2.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SeenDatesUiState(
    val dates: List<Long> = emptyList()
)

class SeenDatesViewModel(
    repo: VocabularyRepository
) : ViewModel() {

    val state: StateFlow<SeenDatesUiState> =
        repo.observeSeenDates()
            .map { dates -> SeenDatesUiState(dates = dates) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SeenDatesUiState())

    class Factory(private val repo: VocabularyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SeenDatesViewModel(repo) as T
    }
}


