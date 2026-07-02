package com.studio71.germanlinia2_b2.data.repo

import android.content.Context
import com.studio71.germanlinia2_b2.data.csv.CsvVocabularyImporter
import com.studio71.germanlinia2_b2.data.image.GoogleImageSearchClient
import com.studio71.germanlinia2_b2.data.local.AppDatabase
import com.studio71.germanlinia2_b2.data.local.DailyStatEntity
import com.studio71.germanlinia2_b2.data.local.ProgressEntity
import com.studio71.germanlinia2_b2.data.local.SeenEventEntity
import com.studio71.germanlinia2_b2.data.local.SeenEventItem
import com.studio71.germanlinia2_b2.data.local.SeenWordEntity
import com.studio71.germanlinia2_b2.data.local.SeenWordItem
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity
import com.studio71.germanlinia2_b2.data.local.WordCommentEntity
import com.studio71.germanlinia2_b2.data.local.WordImageEntity
import com.studio71.germanlinia2_b2.data.local.WordMarkEntity
import com.studio71.germanlinia2_b2.data.local.WordMarker
import com.studio71.germanlinia2_b2.data.sync.SyncStateTracker
import com.studio71.germanlinia2_b2.domain.srs.SrsScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

/**
 * Single entry point for the UI layer. Wraps the DAOs, the CSV seeding,
 * the SRS scheduler and the daily stats bookkeeping.
 */
class VocabularyRepository(
    context: Context,
    private val db: AppDatabase = AppDatabase.get(context),
    private val imageSearchClient: GoogleImageSearchClient = GoogleImageSearchClient()
) {
    private val appContext = context.applicationContext
    private val vocabularyDao = db.vocabularyDao()
    private val progressDao = db.progressDao()
    private val statsDao = db.statsDao()
    private val wordMarkDao = db.wordMarkDao()
    private val seenWordDao = db.seenWordDao()
    private val seenEventDao = db.seenEventDao()
    private val wordCommentDao = db.wordCommentDao()
    private val wordImageDao = db.wordImageDao()

    fun today(): Long = LocalDate.now().toEpochDay()

    /** Import the bundled CSV the first time the app runs. */
    suspend fun seedIfEmpty() {
        if (vocabularyDao.count() == 0) {
            val items = CsvVocabularyImporter.loadFromAssets(appContext)
            if (items.isNotEmpty()) vocabularyDao.insertAll(items)
        } else if (vocabularyDao.countMissingMemoryTips() > 0) {
            // Existing install (post-migration): backfill memory tips from the bundled
            // CSV, updating ONLY that column so progress/comments/marks stay intact.
            val items = CsvVocabularyImporter.loadFromAssets(appContext)
            items.forEach { item ->
                if (item.memoryTips.isNotBlank()) {
                    vocabularyDao.updateMemoryTips(item.id, item.memoryTips)
                }
            }
        }
    }

    // ---- Vocabulary list ----

    fun observeFiltered(
        filter: VocabularyFilter,
        query: String,
        sortMode: SortMode
    ): Flow<List<VocabularyEntity>> = vocabularyDao.observeFiltered(
        level = filter.level,
        book = filter.book,
        chapter = filter.chapter,
        pos = filter.pos,
        grammarGroup = filter.grammarGroup,
        marker = filter.marker?.takeIf { it != WordMarker.NONE }?.value,
        query = query,
        sortMode = sortMode.key
    )

    suspend fun getFilteredSnapshot(
        filter: VocabularyFilter,
        query: String,
        sortMode: SortMode
    ): List<VocabularyEntity> = vocabularyDao.getFiltered(
        level = filter.level,
        book = filter.book,
        chapter = filter.chapter,
        pos = filter.pos,
        grammarGroup = filter.grammarGroup,
        marker = filter.marker?.takeIf { it != WordMarker.NONE }?.value,
        query = query,
        sortMode = sortMode.key
    )

    /** Full source-order snapshot used by game rotation coverage. */
    suspend fun getAllWordsSourceOrder(): List<VocabularyEntity> =
        vocabularyDao.getFiltered(
            level = null,
            book = null,
            chapter = null,
            pos = null,
            grammarGroup = null,
            marker = null,
            query = "",
            sortMode = SortMode.SOURCE.key
        )

    fun observeLevels() = vocabularyDao.observeLevels()
    fun observeBooks() = vocabularyDao.observeBooks()
    fun observeChapters() = vocabularyDao.observeChapters()
    fun observePartsOfSpeech() = vocabularyDao.observePartsOfSpeech()
    fun observeGrammarGroups() = vocabularyDao.observeGrammarGroups()

    suspend fun getById(id: String): VocabularyEntity? = vocabularyDao.getById(id)
    suspend fun findByWord(word: String): VocabularyEntity? = vocabularyDao.findByWord(word)

    fun observeWordImage(wordId: String): Flow<WordImageState> =
        wordImageDao.observeById(wordId).map(::toWordImageState)

    suspend fun ensureWordImage(wordId: String, force: Boolean = false) {
        val word = vocabularyDao.getById(wordId) ?: return
        val existing = wordImageDao.getById(wordId)

        if (!force && existing?.status == WordImageEntity.STATUS_READY) return
        if (!force && existing?.status in setOf(WordImageEntity.STATUS_LOADING, WordImageEntity.STATUS_NO_RESULT, WordImageEntity.STATUS_ERROR)) return

        val now = System.currentTimeMillis()
        wordImageDao.upsert(
            WordImageEntity(
                wordId = wordId,
                imageUrl = existing?.imageUrl,
                query = existing?.query.orEmpty(),
                status = WordImageEntity.STATUS_LOADING,
                lastTriedAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )

        try {
            val result = imageSearchClient.findMeaningImage(word)
            val finalUrl = result.imageUrl ?: existing?.imageUrl
            val status = if (finalUrl.isNullOrBlank()) {
                WordImageEntity.STATUS_NO_RESULT
            } else {
                WordImageEntity.STATUS_READY
            }

            wordImageDao.upsert(
                WordImageEntity(
                    wordId = wordId,
                    imageUrl = finalUrl,
                    query = result.queryUsed,
                    status = status,
                    lastTriedAtEpochMs = now,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {
            val fallbackStatus = if (existing?.imageUrl.isNullOrBlank()) {
                WordImageEntity.STATUS_ERROR
            } else {
                WordImageEntity.STATUS_READY
            }

            wordImageDao.upsert(
                WordImageEntity(
                    wordId = wordId,
                    imageUrl = existing?.imageUrl,
                    query = existing?.query.orEmpty(),
                    status = fallbackStatus,
                    lastTriedAtEpochMs = now,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun retryWordImage(wordId: String) {
        ensureWordImage(wordId, force = true)
    }

    suspend fun prefetchMissingImages(limit: Int = 20) {
        wordImageDao.nextMissingWordIds(limit = limit).forEach { wordId ->
            ensureWordImage(wordId = wordId, force = false)
        }
    }

    // ---- Progress / SRS ----

    fun observeProgress(wordId: String): Flow<ProgressEntity?> = progressDao.observeById(wordId)
    suspend fun getProgress(wordId: String): ProgressEntity? = progressDao.getById(wordId)
    fun observeDue(): Flow<List<VocabularyEntity>> = progressDao.observeDue(today())
    fun observeDueCount(): Flow<Int> = progressDao.observeDueCount(today())
    fun observeLearnedCount(): Flow<Int> = progressDao.observeLearnedCount()

    /** One-shot snapshot of today's due words, used to seed a review session. */
    suspend fun getDueWords(): List<VocabularyEntity> = progressDao.getDue(today())

    /** One-shot due count, used by the daily reminder worker. */
    suspend fun dueCountNow(): Int = progressDao.dueCount(today())

    suspend fun markLearned(wordId: String) {
        val today = today()
        val existing = progressDao.getById(wordId)
        if (existing == null) {
            progressDao.upsert(SrsScheduler.onLearned(wordId, today))
            SyncStateTracker.markLocalMutation(appContext)
            bumpStat(today, learned = 1)
        }
    }

    /**
     * Record that a word detail page was opened.
     * The same word is counted once per day for "seen" stats.
     */
    suspend fun recordWordSeen(wordId: String, autoAddToReview: Boolean) {
        val today = today()
        val seenAt = System.currentTimeMillis()
        val inserted = seenWordDao.insert(
            SeenWordEntity(
                date = today,
                wordId = wordId,
                seenAtEpochMs = seenAt
            )
        )
        if (inserted != -1L) {
            bumpStat(today, seen = 1)
        }

        seenEventDao.insert(
            SeenEventEntity(
                eventId = UUID.randomUUID().toString(),
                wordId = wordId,
                seenAtEpochMs = seenAt
            )
        )
        SyncStateTracker.markLocalMutation(appContext)

        if (autoAddToReview && progressDao.getById(wordId) == null) {
            // Auto-add starts at the "day 2" reminder, then follows 3/7/15/30.
            progressDao.upsert(
                ProgressEntity(
                    wordId = wordId,
                    firstLearned = today,
                    lastReviewed = today,
                    nextDue = today + 2,
                    intervalIndex = 1,
                    box = 1
                )
            )
            SyncStateTracker.markLocalMutation(appContext)
        }
    }

    suspend fun reviewSuccess(wordId: String) {
        val today = today()
        val current = progressDao.getById(wordId) ?: return
        progressDao.upsert(SrsScheduler.onReviewSuccess(current, today))
        SyncStateTracker.markLocalMutation(appContext)
        bumpStat(today, reviewed = 1)
    }

    suspend fun reviewFail(wordId: String) {
        val today = today()
        val current = progressDao.getById(wordId) ?: return
        progressDao.upsert(SrsScheduler.onReviewFail(current, today))
        SyncStateTracker.markLocalMutation(appContext)
        bumpStat(today, reviewed = 1)
    }

    // ---- Difficulty markers (hard / medium / easy) ----

    /** Live marker for one word; emits [WordMarker.NONE] when unmarked. */
    fun observeMarker(wordId: String): Flow<WordMarker> =
        progressMarkerFlow(wordId)

    private fun progressMarkerFlow(wordId: String): Flow<WordMarker> =
        wordMarkDao.observeMarker(wordId).map { WordMarker.fromValue(it) }

    /** Live map of wordId -> marker, used by the list to show colored stars. */
    fun observeMarks(): Flow<Map<String, WordMarker>> =
        wordMarkDao.observeAll().map { marks ->
            marks.associate { it.wordId to WordMarker.fromValue(it.marker) }
        }

    /** Set or clear a word's marker. Passing [WordMarker.NONE] removes the mark. */
    suspend fun setMarker(wordId: String, marker: WordMarker) {
        if (marker == WordMarker.NONE) {
            wordMarkDao.delete(wordId)
        } else {
            wordMarkDao.upsert(WordMarkEntity(wordId, marker.value))
        }
        SyncStateTracker.markLocalMutation(appContext)
    }

    // ---- Stats ----

    fun observeAllStats(): Flow<List<DailyStatEntity>> = statsDao.observeAll()

    fun observeSeenDates(): Flow<List<Long>> = seenWordDao.observeSeenDates()

    fun observeSeenWords(date: Long): Flow<List<SeenWordItem>> = seenWordDao.observeByDate(date)

    fun observeDistinctSeenWordsCount(): Flow<Int> = seenWordDao.observeDistinctWordCount()

    suspend fun getRecentSeenWords(limit: Int = 50): List<SeenWordItem> =
        seenWordDao.getRecent(limit.coerceIn(1, 500))

    suspend fun getRecentSeenEvents(limit: Int = 50): List<SeenEventItem> =
        seenEventDao.getRecent(limit.coerceIn(1, 500))

    suspend fun getAllSeenEvents(): List<SeenEventEntity> = seenEventDao.getAll()

    fun observeComment(wordId: String): Flow<String> =
        wordCommentDao.observeComment(wordId).map { it.orEmpty() }

    suspend fun setComment(wordId: String, comment: String) {
        val normalized = comment.trim()
        if (normalized.isBlank()) {
            wordCommentDao.delete(wordId)
        } else {
            wordCommentDao.upsert(
                WordCommentEntity(
                    wordId = wordId,
                    comment = normalized,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
        }
        SyncStateTracker.markLocalMutation(appContext)
    }

    private suspend fun bumpStat(date: Long, seen: Int = 0, learned: Int = 0, reviewed: Int = 0) {
        val existing = statsDao.getByDate(date) ?: DailyStatEntity(date)
        statsDao.upsert(
            existing.copy(
                wordsSeen = existing.wordsSeen + seen,
                wordsLearned = existing.wordsLearned + learned,
                wordsReviewed = existing.wordsReviewed + reviewed
            )
        )
    }

    private fun toWordImageState(entity: WordImageEntity?): WordImageState {
        val safeUrl = entity?.imageUrl?.trim().orEmpty().takeIf { it.isNotBlank() }
        return when (entity?.status) {
            WordImageEntity.STATUS_READY -> {
                if (safeUrl != null) WordImageState(imageUrl = safeUrl)
                else WordImageState(canRetry = true, message = "Bildlink war ungültig. Bitte erneut suchen.")
            }
            WordImageEntity.STATUS_LOADING -> WordImageState(imageUrl = safeUrl, isLoading = safeUrl == null)
            WordImageEntity.STATUS_NO_RESULT -> WordImageState(
                canRetry = true,
                message = "Kein passendes Bedeutungsbild gefunden."
            )
            WordImageEntity.STATUS_ERROR -> WordImageState(
                canRetry = true,
                message = "Bild konnte nicht geladen werden."
            )
            else -> WordImageState()
        }
    }
}

