package com.studio71.germanlinia2_b2.ui.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studio71.germanlinia2_b2.data.local.ProgressEntity
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity
import com.studio71.germanlinia2_b2.data.local.WordMarker
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CardUiState(
    val word: VocabularyEntity? = null,
    val index: Int = 0,
    val total: Int = 0
) {
    val hasPrevious: Boolean get() = index > 0
    val hasNext: Boolean get() = index < total - 1
}

@OptIn(ExperimentalCoroutinesApi::class)
class WordCardViewModel(
    private val repo: VocabularyRepository,
    startId: String
) : ViewModel() {

    private val _currentId = MutableStateFlow(startId)
    val currentId: StateFlow<String> = _currentId.asStateFlow()

    private val _word = MutableStateFlow<VocabularyEntity?>(null)
    val word: StateFlow<VocabularyEntity?> = _word.asStateFlow()

    val state: StateFlow<CardUiState> =
        combine(_word, _currentId) { w, id ->
            val idx = CardDeck.indexOf(id).coerceAtLeast(0)
            CardUiState(word = w, index = idx, total = CardDeck.size)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CardUiState())

    /** Live progress (last reviewed, due date) for the current word. */
    val progress: StateFlow<ProgressEntity?> =
        _currentId.flatMapLatest { id -> repo.observeProgress(id) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Live difficulty marker (red/yellow/blue star) for the current word. */
    val marker: StateFlow<WordMarker> =
        _currentId.flatMapLatest { id -> repo.observeMarker(id) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WordMarker.NONE)

    /** Live personal comment for the current word (blank when none). */
    val comment: StateFlow<String> =
        _currentId.flatMapLatest { id -> repo.observeComment(id) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    init {
        load(startId)
    }

    private fun load(id: String) {
        _currentId.value = id
        viewModelScope.launch { _word.value = repo.getById(id) }
    }

    fun next() {
        val idx = CardDeck.indexOf(_currentId.value)
        CardDeck.idAt(idx + 1)?.let { load(it) }
    }

    fun previous() {
        val idx = CardDeck.indexOf(_currentId.value)
        CardDeck.idAt(idx - 1)?.let { load(it) }
    }

    fun markLearned() {
        viewModelScope.launch { repo.markLearned(_currentId.value) }
    }

    fun reviewSuccess() {
        viewModelScope.launch { repo.reviewSuccess(_currentId.value) }
    }

    fun reviewFail() {
        viewModelScope.launch { repo.reviewFail(_currentId.value) }
    }

    fun onWordSeen(autoAddToReview: Boolean) {
        viewModelScope.launch { repo.recordWordSeen(_currentId.value, autoAddToReview) }
    }

    suspend fun lookup(wordOrPhrase: String): VocabularyEntity? = repo.findByWord(wordOrPhrase)

    /** Set or clear the difficulty marker for the current word. */
    fun setMarker(marker: WordMarker) {
        viewModelScope.launch { repo.setMarker(_currentId.value, marker) }
    }

    fun setComment(comment: String) {
        viewModelScope.launch { repo.setComment(_currentId.value, comment) }
    }

    class Factory(
        private val repo: VocabularyRepository,
        private val startId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WordCardViewModel(repo, startId) as T
    }
}

