package com.studio71.germanlinia2_b2.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository
import com.studio71.germanlinia2_b2.data.repo.WordImageState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReviewUiState(
    val loading: Boolean = true,
    val word: VocabularyEntity? = null,
    val index: Int = 0,
    val total: Int = 0,
    val knownCount: Int = 0,
    val againCount: Int = 0,
    val finished: Boolean = false
) {
    val reviewed: Int get() = knownCount + againCount
}

/**
 * Drives a single spaced-repetition review session over the words that are due today.
 * Each [grade] call reschedules the word via the repository and advances to the next.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModel(
    private val repo: VocabularyRepository
) : ViewModel() {

    private var deck: List<VocabularyEntity> = emptyList()

    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()
    private val _currentWordId = MutableStateFlow<String?>(null)

    val imageState: StateFlow<WordImageState> =
        _currentWordId.flatMapLatest { id ->
            if (id == null) {
                flowOf(WordImageState())
            } else {
                repo.observeWordImage(id)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WordImageState())

    init {
        viewModelScope.launch {
            deck = repo.getDueWords()
            _state.value = ReviewUiState(
                loading = false,
                word = deck.firstOrNull(),
                index = 0,
                total = deck.size,
                finished = deck.isEmpty()
            )
            val firstId = deck.firstOrNull()?.id
            _currentWordId.value = firstId
            if (firstId != null) repo.ensureWordImage(firstId)
        }
    }

    /** Grade the current word: true = "Gewusst", false = "Nochmal". */
    fun grade(known: Boolean) {
        val current = _state.value
        val word = current.word ?: return
        viewModelScope.launch {
            if (known) repo.reviewSuccess(word.id) else repo.reviewFail(word.id)

            val nextIndex = current.index + 1
            _state.value = current.copy(
                index = nextIndex,
                word = deck.getOrNull(nextIndex),
                knownCount = current.knownCount + if (known) 1 else 0,
                againCount = current.againCount + if (known) 0 else 1,
                finished = nextIndex >= deck.size
            )
            val nextWordId = deck.getOrNull(nextIndex)?.id
            _currentWordId.value = nextWordId
            if (nextWordId != null) repo.ensureWordImage(nextWordId)
        }
    }

    suspend fun lookup(wordOrPhrase: String): VocabularyEntity? = repo.findByWord(wordOrPhrase)

    fun retryImage() {
        val id = _currentWordId.value ?: return
        viewModelScope.launch { repo.retryWordImage(id) }
    }

    fun observeWordImage(wordId: String): Flow<WordImageState> = repo.observeWordImage(wordId)

    fun ensureWordImage(wordId: String) {
        viewModelScope.launch { repo.ensureWordImage(wordId) }
    }

    fun retryWordImage(wordId: String) {
        viewModelScope.launch { repo.retryWordImage(wordId) }
    }

    class Factory(private val repo: VocabularyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReviewViewModel(repo) as T
    }
}

