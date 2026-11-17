package com.studio71.germanlinia2_b2.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studio71.germanlinia2_b2.data.local.ArchivedWordItem
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Words that left the review plan (reset or finished), with a way back into it. */
class ArchiveViewModel(
    private val repo: VocabularyRepository
) : ViewModel() {

    val words: StateFlow<List<ArchivedWordItem>> =
        repo.observeArchive()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restore(wordId: String) {
        viewModelScope.launch { repo.restoreFromArchive(wordId) }
    }

    fun restoreAll() {
        viewModelScope.launch { repo.restoreAllFromArchive() }
    }

    class Factory(private val repo: VocabularyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ArchiveViewModel(repo) as T
    }
}
