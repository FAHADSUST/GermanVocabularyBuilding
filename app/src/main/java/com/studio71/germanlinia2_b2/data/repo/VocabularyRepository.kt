package com.studio71.germanlinia2_b2.data.repo

import android.content.Context
import com.studio71.germanlinia2_b2.data.csv.CsvVocabularyImporter
import com.studio71.germanlinia2_b2.data.local.AppDatabase
import com.studio71.germanlinia2_b2.data.local.DailyStatEntity
import com.studio71.germanlinia2_b2.data.local.ProgressEntity
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

    private suspend fun bumpStat(date: Long, learned: Int = 0, reviewed: Int = 0) {
        val existing = statsDao.getByDate(date) ?: DailyStatEntity(date)
        statsDao.upsert(
            existing.copy(
                wordsLearned = existing.wordsLearned + learned,
                wordsReviewed = existing.wordsReviewed + reviewed
            )
        )
    }
}

