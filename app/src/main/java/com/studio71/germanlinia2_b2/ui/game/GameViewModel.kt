package com.studio71.germanlinia2_b2.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.studio71.germanlinia2_b2.data.local.SeenEventItem
import com.studio71.germanlinia2_b2.data.local.SeenWordItem
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository
import com.studio71.germanlinia2_b2.data.settings.SettingsStore
import com.studio71.germanlinia2_b2.domain.game.GameMode
import com.studio71.germanlinia2_b2.domain.game.GameSessionPlanner
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class RecentFilter {
    ALL,
    UNIQUE,
    EVENTS
}

enum class RecentSource {
    UNIQUE,
    EVENT
}

data class RecentReviewRow(
    val key: String,
    val source: RecentSource,
    val wordId: String,
    val displayWord: String,
    val english: String,
    val seenAtEpochMs: Long,
    val reviewed: Boolean = false
)

data class GameUiState(
    val loading: Boolean = true,
    val mode: GameMode = GameMode.MATCHING,
    val title: String = GameMode.MATCHING.title,
    val batch: List<VocabularyEntity> = emptyList(),
    val matchingLeftColumn: List<VocabularyEntity> = emptyList(),
    val matchingRightColumn: List<VocabularyEntity> = emptyList(),
    val selectedLeftId: String? = null,
    val selectedRightId: String? = null,
    val matchedLeftToRight: Map<String, String> = emptyMap(),
    val clozeIndex: Int = 0,
    val clozeInput: String = "",
    val clozeShowChoices: Boolean = false,
    val clozeChoiceWords: List<VocabularyEntity> = emptyList(),
    val reverseIndex: Int = 0,
    val reverseRevealed: Boolean = false,
    val recentFilter: RecentFilter = RecentFilter.ALL,
    val recentRows: List<RecentReviewRow> = emptyList(),
    val knownCount: Int = 0,
    val againCount: Int = 0,
    val finished: Boolean = false,
    val message: String = ""
)

class GameViewModel(
    private val repo: VocabularyRepository,
    private val settings: SettingsStore,
    modeRoute: String,
    private val sessionId: String? = null
) : ViewModel() {

    private val mode = GameMode.fromRoute(modeRoute)
    private var allWords: List<VocabularyEntity> = emptyList()
    private var activeSessionId: String = sessionId ?: java.util.UUID.randomUUID().toString()

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(
        GameUiState(mode = mode, title = mode.title)
    )
    val state: kotlinx.coroutines.flow.StateFlow<GameUiState> = _state

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            if (sessionId != null) {
                val savedGame = repo.getActiveGame(sessionId)
                if (savedGame != null) {
                    val wordIds = savedGame.wordIds.split(",").filter { it.isNotBlank() }
                    val batch = wordIds.mapNotNull { repo.getById(it) }
                    allWords = repo.getAllWordsSourceOrder()
                    
                    when (mode) {
                        GameMode.MATCHING -> {
                            val matchedIds = savedGame.extraData.split(",").filter { it.isNotBlank() }.toSet()
                            val unmatchedLeft = batch.filterNot { it.id in matchedIds }
                            _state.value = _state.value.copy(
                                loading = false,
                                batch = batch,
                                matchingLeftColumn = unmatchedLeft,
                                matchingRightColumn = unmatchedLeft.shuffled(),
                                matchedLeftToRight = matchedIds.associateWith { it },
                                knownCount = savedGame.knownCount,
                                againCount = savedGame.againCount,
                                finished = unmatchedLeft.isEmpty() && batch.isNotEmpty()
                            )
                        }

                        GameMode.CLOZE -> {
                            _state.value = _state.value.copy(
                                loading = false,
                                batch = batch,
                                clozeIndex = savedGame.progressIndex,
                                knownCount = savedGame.knownCount,
                                againCount = savedGame.againCount,
                                finished = savedGame.progressIndex >= batch.size && batch.isNotEmpty()
                            )
                        }

                        GameMode.REVERSE_RECALL -> {
                            _state.value = _state.value.copy(
                                loading = false,
                                batch = batch,
                                reverseIndex = savedGame.progressIndex,
                                knownCount = savedGame.knownCount,
                                againCount = savedGame.againCount,
                                finished = savedGame.progressIndex >= batch.size && batch.isNotEmpty()
                            )
                        }

                        GameMode.RECENT -> {
                        }
                    }
                    return@launch
                }
            }

            when (mode) {
                GameMode.MATCHING -> {
                    val batch = loadMixedBatch()
                    _state.value = _state.value.copy(
                        loading = false,
                        batch = batch,
                        matchingLeftColumn = batch,
                        matchingRightColumn = batch.shuffled(),
                        message = if (batch.isEmpty()) "Keine Worter verfugbar." else ""
                    )
                    saveOrUpdateActiveGame()
                }

                GameMode.CLOZE -> {
                    val batch = loadMixedBatch()
                    _state.value = _state.value.copy(
                        loading = false,
                        batch = batch,
                        message = if (batch.isEmpty()) "Keine Worter verfugbar." else ""
                    )
                    saveOrUpdateActiveGame()
                }

                GameMode.REVERSE_RECALL -> {
                    val batch = loadMixedBatch()
                    _state.value = _state.value.copy(
                        loading = false,
                        batch = batch,
                        message = if (batch.isEmpty()) "Keine Worter verfugbar." else ""
                    )
                    saveOrUpdateActiveGame()
                }

                GameMode.RECENT -> {
                    val uniqueRows = repo.getRecentSeenWords(50).map { it.toRow() }
                    val eventRows = repo.getRecentSeenEvents(50).map { it.toRow() }
                    val merged = (uniqueRows + eventRows)
                        .sortedByDescending { it.seenAtEpochMs }
                        .distinctBy { it.wordId }
                        .take(50)
                    _state.value = _state.value.copy(
                        loading = false,
                        recentRows = merged,
                        message = if (merged.isEmpty()) "Noch keine angesehenen Worter." else ""
                    )
                }
            }
        }
    }

    private suspend fun loadMixedBatch(): List<VocabularyEntity> {
        allWords = repo.getAllWordsSourceOrder()
        val currentSettings = settings.state.value
        val seenOrMarkedIds = repo.getSeenOrMarkedWordIds()
        val mixed = GameSessionPlanner.mixedCoverageSlice(
            allWords = allWords,
            seenOrMarkedIds = seenOrMarkedIds,
            seenMarkedCursor = currentSettings.gameSeenMarkedCoverageCursor,
            newCursor = currentSettings.gameNewCoverageCursor,
            size = 10,
            seenMarkedSharePercent = 70
        )
        settings.setGameSeenMarkedCoverageCursor(mixed.nextSeenMarkedCursor)
        settings.setGameNewCoverageCursor(mixed.nextNewCursor)
        return mixed.batch
    }

    fun selectMatchingLeft(wordId: String) {
        val s = _state.value
        if (s.matchingLeftColumn.none { it.id == wordId }) return
        _state.value = s.copy(selectedLeftId = wordId)
        maybeResolveMatching()
    }

    fun selectMatchingRight(wordId: String) {
        val s = _state.value
        if (s.matchingRightColumn.none { it.id == wordId }) return
        _state.value = s.copy(selectedRightId = wordId)
        maybeResolveMatching()
    }

    private fun maybeResolveMatching() {
        val s = _state.value
        val left = s.selectedLeftId ?: return
        val right = s.selectedRightId ?: return
        val leftExists = s.matchingLeftColumn.any { it.id == left }
        val rightExists = s.matchingRightColumn.any { it.id == right }
        if (!leftExists || !rightExists) {
            _state.value = s.copy(selectedLeftId = null, selectedRightId = null)
            return
        }

        if (left == right) {
            val matches = s.matchedLeftToRight + (left to right)
            val nextLeft = s.matchingLeftColumn.filterNot { it.id == left }
            val nextRight = s.matchingRightColumn.filterNot { it.id == right }
            _state.value = s.copy(
                selectedLeftId = null,
                selectedRightId = null,
                matchedLeftToRight = matches,
                matchingLeftColumn = nextLeft,
                matchingRightColumn = nextRight,
                knownCount = s.knownCount + 1,
                finished = nextLeft.isEmpty() && s.batch.isNotEmpty()
            )
            viewModelScope.launch {
                applyOutcome(left, known = true)
                saveOrUpdateActiveGame()
            }
        } else {
            _state.value = s.copy(
                selectedLeftId = null,
                selectedRightId = null,
                againCount = s.againCount + 1
            )
            viewModelScope.launch {
                applyOutcome(left, known = false)
                saveOrUpdateActiveGame()
            }
        }
    }

    fun onClozeInputChange(value: String) {
        _state.value = _state.value.copy(clozeInput = value)
    }

    fun submitCloze() {
        val s = _state.value
        val word = s.batch.getOrNull(s.clozeIndex) ?: return
        if (isAnswerCorrect(s.clozeInput, word)) {
            viewModelScope.launch {
                applyOutcome(word.id, known = true)
                advanceCloze(known = true)
            }
            return
        }

        if (!s.clozeShowChoices) {
            val distractors = allWords
                .asSequence()
                .filter { it.id != word.id }
                .shuffled()
                .take(3)
                .toList()
            val options = (distractors + word).shuffled(Random)
            _state.value = s.copy(clozeShowChoices = true, clozeChoiceWords = options)
        }
    }

    fun chooseClozeOption(wordId: String) {
        val s = _state.value
        val current = s.batch.getOrNull(s.clozeIndex) ?: return
        val known = wordId == current.id
        viewModelScope.launch {
            applyOutcome(current.id, known)
            advanceCloze(known)
        }
    }

    private fun advanceCloze(known: Boolean) {
        val s = _state.value
        val nextIndex = s.clozeIndex + 1
        _state.value = s.copy(
            clozeIndex = nextIndex,
            clozeInput = "",
            clozeShowChoices = false,
            clozeChoiceWords = emptyList(),
            knownCount = s.knownCount + if (known) 1 else 0,
            againCount = s.againCount + if (known) 0 else 1,
            finished = nextIndex >= s.batch.size && s.batch.isNotEmpty()
        )
        viewModelScope.launch {
            saveOrUpdateActiveGame()
        }
    }

    fun revealReverse() {
        _state.value = _state.value.copy(reverseRevealed = true)
    }

    fun gradeReverse(known: Boolean) {
        val s = _state.value
        val current = s.batch.getOrNull(s.reverseIndex) ?: return
        viewModelScope.launch {
            applyOutcome(current.id, known)
            val next = s.reverseIndex + 1
            _state.value = s.copy(
                reverseIndex = next,
                reverseRevealed = false,
                knownCount = s.knownCount + if (known) 1 else 0,
                againCount = s.againCount + if (known) 0 else 1,
                finished = next >= s.batch.size && s.batch.isNotEmpty()
            )
            saveOrUpdateActiveGame()
        }
    }

    private suspend fun saveOrUpdateActiveGame() {
        val s = _state.value
        if (s.mode == GameMode.RECENT || s.batch.isEmpty()) return

        if (s.finished) {
            repo.deleteActiveGame(activeSessionId)
            return
        }

        val wordIds = s.batch.joinToString(",") { it.id }
        val progressIndex = when (s.mode) {
            GameMode.CLOZE -> s.clozeIndex
            GameMode.REVERSE_RECALL -> s.reverseIndex
            else -> s.matchedLeftToRight.size
        }
        val extraData = when (s.mode) {
            GameMode.MATCHING -> s.matchedLeftToRight.keys.joinToString(",")
            else -> ""
        }

        repo.saveActiveGame(
            com.studio71.germanlinia2_b2.data.local.ActiveGameEntity(
                id = activeSessionId,
                mode = s.mode.routeValue,
                createdAtEpochMs = System.currentTimeMillis(),
                wordIds = wordIds,
                progressIndex = progressIndex,
                knownCount = s.knownCount,
                againCount = s.againCount,
                extraData = extraData
            )
        )
    }

    fun setRecentFilter(filter: RecentFilter) {
        _state.value = _state.value.copy(recentFilter = filter)
    }

    fun gradeRecent(rowKey: String, known: Boolean) {
        val s = _state.value
        val row = s.recentRows.firstOrNull { it.key == rowKey } ?: return
        if (row.reviewed) return

        viewModelScope.launch {
            applyOutcome(row.wordId, known)
            _state.value = s.copy(
                recentRows = s.recentRows.map {
                    if (it.key == rowKey) it.copy(reviewed = true) else it
                },
                knownCount = s.knownCount + if (known) 1 else 0,
                againCount = s.againCount + if (known) 0 else 1
            )
        }
    }

    fun filteredRecentRows(): List<RecentReviewRow> {
        val s = _state.value
        return when (s.recentFilter) {
            RecentFilter.ALL -> s.recentRows
            RecentFilter.UNIQUE -> s.recentRows.filter { it.source == RecentSource.UNIQUE }
            RecentFilter.EVENTS -> s.recentRows.filter { it.source == RecentSource.EVENT }
        }
    }

    fun currentClozeWord(): VocabularyEntity? = state.value.batch.getOrNull(state.value.clozeIndex)

    fun currentReverseWord(): VocabularyEntity? = state.value.batch.getOrNull(state.value.reverseIndex)

    private suspend fun applyOutcome(wordId: String, known: Boolean) {
        val progress = repo.getProgress(wordId)
        if (progress == null) {
            repo.markLearned(wordId)
            if (!known) {
                repo.reviewFail(wordId)
            }
            return
        }
        if (known) repo.reviewSuccess(wordId) else repo.reviewFail(wordId)
    }

    private fun isAnswerCorrect(input: String, word: VocabularyEntity): Boolean {
        val normalized = input.trim().lowercase()
        if (normalized.isBlank()) return false
        val headword = word.word.trim().lowercase()
        val withArticle = word.displayWord.trim().lowercase()
        return normalized == headword || normalized == withArticle
    }

    class Factory(
        private val repo: VocabularyRepository,
        private val settings: SettingsStore,
        private val modeRoute: String,
        private val sessionId: String? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GameViewModel(repo, settings, modeRoute, sessionId) as T
    }
}

private fun SeenWordItem.toRow(): RecentReviewRow =
    RecentReviewRow(
        key = "unique-$wordId",
        source = RecentSource.UNIQUE,
        wordId = wordId,
        displayWord = displayWord,
        english = english,
        seenAtEpochMs = seenAtEpochMs
    )

private fun SeenEventItem.toRow(): RecentReviewRow =
    RecentReviewRow(
        key = "event-$eventId",
        source = RecentSource.EVENT,
        wordId = wordId,
        displayWord = displayWord,
        english = english,
        seenAtEpochMs = seenAtEpochMs
    )


