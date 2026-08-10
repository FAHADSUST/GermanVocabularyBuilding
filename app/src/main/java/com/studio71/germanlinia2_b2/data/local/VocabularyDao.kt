package com.studio71.germanlinia2_b2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VocabularyEntity>)

    @Query("SELECT COUNT(*) FROM vocabulary")
    suspend fun count(): Int

    @Query("DELETE FROM vocabulary")
    suspend fun clearTable()

    /** How many rows still have no memory tip (used to backfill existing installs). */
    @Query("SELECT COUNT(*) FROM vocabulary WHERE memoryTips = '' OR memoryTips IS NULL")
    suspend fun countMissingMemoryTips(): Int

    /** Update only the memory tip column, preserving all other data + progress tables. */
    @Query("UPDATE vocabulary SET memoryTips = :tips WHERE id = :id")
    suspend fun updateMemoryTips(id: String, tips: String)

    @Query("SELECT * FROM vocabulary WHERE id = :id")
    suspend fun getById(id: String): VocabularyEntity?

    @Query(
        "SELECT * FROM vocabulary " +
            "WHERE word = :word COLLATE NOCASE " +
            "OR (article || ' ' || word) = :word COLLATE NOCASE LIMIT 1"
    )
    suspend fun findByWord(word: String): VocabularyEntity?

    /**
     * Flexible filtered + searched query. Pass empty string / null to ignore a filter.
     * Sorting is handled in the repository by switching the query variant.
     */
    @Query(
        """
        SELECT vocabulary.* FROM vocabulary
        LEFT JOIN word_mark ON word_mark.wordId = vocabulary.id
        WHERE (:level IS NULL OR level = :level)
          AND (:book IS NULL OR book = :book)
          AND (:chapter IS NULL OR chapter = :chapter)
          AND (:pos IS NULL OR pos = :pos)
          AND (:grammarGroup IS NULL OR grammarGroup = :grammarGroup)
          AND (:marker IS NULL OR word_mark.marker = :marker)
          AND (
                :query = '' OR
                word LIKE '%' || :query || '%' OR
                english LIKE '%' || :query || '%' OR
                germanMeaning LIKE '%' || :query || '%'
          )
        ORDER BY
          CASE WHEN :query != '' THEN
            CASE
              -- Direct exact match in word (with or without article) or sortKey
              WHEN word = :query COLLATE NOCASE OR (article || ' ' || word) = :query COLLATE NOCASE OR sortKey = :query COLLATE NOCASE THEN 1
              -- Direct prefix match (word starts with query)
              WHEN word LIKE :query || '%' OR sortKey LIKE :query || '%' THEN 2
              -- Direct substring match (query is in word)
              WHEN word LIKE '%' || :query || '%' OR sortKey LIKE '%' || :query || '%' THEN 3
              -- Match in description or other places
              ELSE 4
            END
          ELSE 1 END ASC,
          CASE WHEN :sortMode = 'ALPHA' THEN sortKey END ASC,
          CASE WHEN :sortMode = 'ALPHA_DESC' THEN sortKey END DESC,
          CASE WHEN :sortMode = 'FREQUENCY' THEN frequencyRank END ASC,
          CASE WHEN :sortMode = 'SOURCE' THEN orderIndex END ASC,
          CASE WHEN :sortMode = 'CHAPTER' THEN level END ASC,
          CASE WHEN :sortMode = 'CHAPTER' THEN book END ASC,
          CASE WHEN :sortMode = 'CHAPTER' THEN chapter END ASC,
          orderIndex ASC
        """
    )
    fun observeFiltered(
        level: String?,
        book: String?,
        chapter: String?,
        pos: String?,
        grammarGroup: String?,
        marker: Int?,
        query: String,
        sortMode: String
    ): Flow<List<VocabularyEntity>>

    @Query(
        """
        SELECT vocabulary.* FROM vocabulary
        LEFT JOIN word_mark ON word_mark.wordId = vocabulary.id
        WHERE (:level IS NULL OR level = :level)
          AND (:book IS NULL OR book = :book)
          AND (:chapter IS NULL OR chapter = :chapter)
          AND (:pos IS NULL OR pos = :pos)
          AND (:grammarGroup IS NULL OR grammarGroup = :grammarGroup)
          AND (:marker IS NULL OR word_mark.marker = :marker)
          AND (
                :query = '' OR
                word LIKE '%' || :query || '%' OR
                english LIKE '%' || :query || '%' OR
                germanMeaning LIKE '%' || :query || '%'
          )
        ORDER BY
          CASE WHEN :query != '' THEN
            CASE
              -- Direct exact match in word (with or without article) or sortKey
              WHEN word = :query COLLATE NOCASE OR (article || ' ' || word) = :query COLLATE NOCASE OR sortKey = :query COLLATE NOCASE THEN 1
              -- Direct prefix match (word starts with query)
              WHEN word LIKE :query || '%' OR sortKey LIKE :query || '%' THEN 2
              -- Direct substring match (query is in word)
              WHEN word LIKE '%' || :query || '%' OR sortKey LIKE '%' || :query || '%' THEN 3
              -- Match in description or other places
              ELSE 4
            END
          ELSE 1 END ASC,
          CASE WHEN :sortMode = 'ALPHA' THEN sortKey END ASC,
          CASE WHEN :sortMode = 'ALPHA_DESC' THEN sortKey END DESC,
          CASE WHEN :sortMode = 'FREQUENCY' THEN frequencyRank END ASC,
          CASE WHEN :sortMode = 'SOURCE' THEN orderIndex END ASC,
          CASE WHEN :sortMode = 'CHAPTER' THEN level END ASC,
          CASE WHEN :sortMode = 'CHAPTER' THEN book END ASC,
          CASE WHEN :sortMode = 'CHAPTER' THEN chapter END ASC,
          orderIndex ASC
        """
    )
    suspend fun getFiltered(
        level: String?,
        book: String?,
        chapter: String?,
        pos: String?,
        grammarGroup: String?,
        marker: Int?,
        query: String,
        sortMode: String
    ): List<VocabularyEntity>

    @Query("SELECT DISTINCT level FROM vocabulary ORDER BY level")
    fun observeLevels(): Flow<List<String>>

    @Query("SELECT DISTINCT book FROM vocabulary ORDER BY book")
    fun observeBooks(): Flow<List<String>>

    @Query("SELECT DISTINCT chapter FROM vocabulary ORDER BY level, book, chapter")
    fun observeChapters(): Flow<List<String>>

    @Query("SELECT DISTINCT pos FROM vocabulary WHERE pos != '' ORDER BY pos")
    fun observePartsOfSpeech(): Flow<List<String>>

    @Query("SELECT DISTINCT grammarGroup FROM vocabulary WHERE grammarGroup != '' ORDER BY grammarGroup")
    fun observeGrammarGroups(): Flow<List<String>>
}

