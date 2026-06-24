package com.studio71.germanlinia2_b2.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studio71.germanlinia2_b2.data.local.SeenWordItem
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SeenWordsByDateUiState(
    val date: Long,
    val words: List<SeenWordItem> = emptyList()
)

class SeenWordsByDateViewModel(
    repo: VocabularyRepository,
    private val date: Long
) : ViewModel() {

    val state: StateFlow<SeenWordsByDateUiState> =
        repo.observeSeenWords(date)
            .map { words -> SeenWordsByDateUiState(date = date, words = words) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                SeenWordsByDateUiState(date = date)
            )

    class Factory(
        private val repo: VocabularyRepository,
        private val date: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SeenWordsByDateViewModel(repo, date) as T
    }
}


