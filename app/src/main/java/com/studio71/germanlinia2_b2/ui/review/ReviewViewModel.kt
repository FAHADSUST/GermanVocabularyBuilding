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

data class ReviewItemState(
    val word: VocabularyEntity,
    val isReviewed: Boolean = false,
    val isKnown: Boolean? = null
)

data class ReviewUiState(
    val loading: Boolean = true,
    val word: VocabularyEntity? = null,
    val index: Int = 0,
    val total: Int = 0,
    val knownCount: Int = 0,
    val againCount: Int = 0,
    val finished: Boolean = false,
    val items: List<ReviewItemState> = emptyList()
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

    private var reviewItems: List<ReviewItemState> = emptyList()

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
            val deck = repo.getDueWords()
            reviewItems = deck.map { ReviewItemState(it) }
            _state.value = ReviewUiState(
                loading = false,
                word = reviewItems.firstOrNull()?.word,
                index = 0,
                total = reviewItems.size,
                finished = reviewItems.isEmpty(),
                items = reviewItems
            )
            val firstId = reviewItems.firstOrNull()?.word?.id
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

            reviewItems = reviewItems.map {
                if (it.word.id == word.id) it.copy(isReviewed = true, isKnown = known) else it
            }

            val nextWord = reviewItems.firstOrNull { !it.isReviewed }?.word
            val revisedKnownCount = current.knownCount + if (known) 1 else 0
            val revisedAgainCount = current.againCount + if (known) 0 else 1
            val reviewedCount = reviewItems.count { it.isReviewed }

            _state.value = current.copy(
                index = reviewedCount,
                word = nextWord,
                knownCount = revisedKnownCount,
                againCount = revisedAgainCount,
                finished = reviewItems.all { it.isReviewed },
                items = reviewItems
            )
            val nextWordId = nextWord?.id
            _currentWordId.value = nextWordId
            if (nextWordId != null) repo.ensureWordImage(nextWordId)
        }
    }

    /** Tap-to-show: reveal the meaning and count it as a successful review. */
    fun revealAndReviewListWord(wordId: String) {
        val current = _state.value
        val existingItem = reviewItems.firstOrNull { it.word.id == wordId } ?: return
        if (existingItem.isReviewed) return

        viewModelScope.launch {
            repo.reviewSuccess(wordId)

            reviewItems = reviewItems.map {
                if (it.word.id == wordId) it.copy(isReviewed = true, isKnown = true) else it
            }

            val nextWordForCard = reviewItems.firstOrNull { !it.isReviewed }?.word
            val revisedKnownCount = current.knownCount + 1
            val reviewedCount = reviewItems.count { it.isReviewed }

            _state.value = current.copy(
                index = reviewedCount,
                word = nextWordForCard,
                knownCount = revisedKnownCount,
                finished = reviewItems.all { it.isReviewed },
                items = reviewItems
            )

            val nextWordId = nextWordForCard?.id
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

