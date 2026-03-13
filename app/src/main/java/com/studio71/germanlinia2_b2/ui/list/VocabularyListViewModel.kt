package com.studio71.germanlinia2_b2.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity
import com.studio71.germanlinia2_b2.data.repo.SortMode
import com.studio71.germanlinia2_b2.data.repo.VocabularyFilter
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class FilterOptions(
    val levels: List<String> = emptyList(),
    val books: List<String> = emptyList(),
    val chapters: List<String> = emptyList(),
    val partsOfSpeech: List<String> = emptyList(),
    val grammarGroups: List<String> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class VocabularyListViewModel(
    private val repo: VocabularyRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(VocabularyFilter())
    val filter: StateFlow<VocabularyFilter> = _filter.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.CHAPTER)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    val words: StateFlow<List<VocabularyEntity>> =
        combine(_filter, _query, _sortMode) { f, q, s -> Triple(f, q, s) }
            .flatMapLatest { (f, q, s) -> repo.observeFiltered(f, q, s) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val options: StateFlow<FilterOptions> = combine(
        repo.observeLevels(),
        repo.observeBooks(),
        repo.observeChapters(),
        repo.observePartsOfSpeech(),
        repo.observeGrammarGroups()
    ) { levels, books, chapters, pos, groups ->
        FilterOptions(levels, books, chapters, pos, groups)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FilterOptions())

    val dueCount: StateFlow<Int> =
        repo.observeDueCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val learnedCount: StateFlow<Int> =
        repo.observeLearnedCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun onQueryChange(value: String) { _query.value = value }
    fun setFilter(filter: VocabularyFilter) { _filter.value = filter }
    fun setSortMode(mode: SortMode) { _sortMode.value = mode }
    fun clearFilters() { _filter.value = VocabularyFilter() }

    class Factory(private val repo: VocabularyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            VocabularyListViewModel(repo) as T
    }
}

