package com.studio71.germanlinia2_b2.data.repo

import android.content.Context
import com.studio71.germanlinia2_b2.data.csv.CsvVocabularyImporter
import com.studio71.germanlinia2_b2.data.local.AppDatabase
import com.studio71.germanlinia2_b2.data.local.DailyStatEntity
import com.studio71.germanlinia2_b2.data.local.ProgressEntity
import com.studio71.germanlinia2_b2.data.local.SeenWordEntity
import com.studio71.germanlinia2_b2.data.local.SeenWordItem
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity
import com.studio71.germanlinia2_b2.data.local.WordMarkEntity
import com.studio71.germanlinia2_b2.data.local.WordMarker
import com.studio71.germanlinia2_b2.domain.srs.SrsScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Single entry point for the UI layer. Wraps the DAOs, the CSV seeding,
 * the SRS scheduler and the daily stats bookkeeping.
 */
class VocabularyRepository(
    context: Context,
    private val db: AppDatabase = AppDatabase.get(context)
) {
    private val appContext = context.applicationContext
    private val vocabularyDao = db.vocabularyDao()
    private val progressDao = db.progressDao()
    private val statsDao = db.statsDao()
    private val wordMarkDao = db.wordMarkDao()
    private val seenWordDao = db.seenWordDao()

    fun today(): Long = LocalDate.now().toEpochDay()

    /** Import the bundled CSV the first time the app runs. */
    suspend fun seedIfEmpty() {
        if (vocabularyDao.count() == 0) {
            val items = CsvVocabularyImporter.loadFromAssets(appContext)
            if (items.isNotEmpty()) vocabularyDao.insertAll(items)
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

    fun observeLevels() = vocabularyDao.observeLevels()
    fun observeBooks() = vocabularyDao.observeBooks()
    fun observeChapters() = vocabularyDao.observeChapters()
    fun observePartsOfSpeech() = vocabularyDao.observePartsOfSpeech()
    fun observeGrammarGroups() = vocabularyDao.observeGrammarGroups()

    suspend fun getById(id: String): VocabularyEntity? = vocabularyDao.getById(id)
    suspend fun findByWord(word: String): VocabularyEntity? = vocabularyDao.findByWord(word)

    // ---- Progress / SRS ----

    fun observeProgress(wordId: String): Flow<ProgressEntity?> = progressDao.observeById(wordId)
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
            bumpStat(today, learned = 1)
        }
    }

    /**
     * Record that a word detail page was opened.
     * The same word is counted once per day for "seen" stats.
     */
    suspend fun recordWordSeen(wordId: String, autoAddToReview: Boolean) {
        val today = today()
        val inserted = seenWordDao.insert(
            SeenWordEntity(
                date = today,
                wordId = wordId,
                seenAtEpochMs = System.currentTimeMillis()
            )
        )
        if (inserted != -1L) {
            bumpStat(today, seen = 1)
        }

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
        }
    }

    suspend fun reviewSuccess(wordId: String) {
        val today = today()
        val current = progressDao.getById(wordId) ?: return
        progressDao.upsert(SrsScheduler.onReviewSuccess(current, today))
        bumpStat(today, reviewed = 1)
    }

    suspend fun reviewFail(wordId: String) {
        val today = today()
        val current = progressDao.getById(wordId) ?: return
        progressDao.upsert(SrsScheduler.onReviewFail(current, today))
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
    }

    // ---- Stats ----

    fun observeAllStats(): Flow<List<DailyStatEntity>> = statsDao.observeAll()

    fun observeSeenWords(date: Long): Flow<List<SeenWordItem>> = seenWordDao.observeByDate(date)

    fun observeDistinctSeenWordsCount(): Flow<Int> = seenWordDao.observeDistinctWordCount()

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
}

