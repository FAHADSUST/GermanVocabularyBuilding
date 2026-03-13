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

    @Query("SELECT * FROM vocabulary WHERE id = :id")
    suspend fun getById(id: String): VocabularyEntity?

    @Query("SELECT * FROM vocabulary WHERE word = :word OR (article || ' ' || word) = :word LIMIT 1")
    suspend fun findByWord(word: String): VocabularyEntity?

    /**
     * Flexible filtered + searched query. Pass empty string / null to ignore a filter.
     * Sorting is handled in the repository by switching the query variant.
     */
    @Query(
        """
        SELECT * FROM vocabulary
        WHERE (:level IS NULL OR level = :level)
          AND (:book IS NULL OR book = :book)
          AND (:chapter IS NULL OR chapter = :chapter)
          AND (:pos IS NULL OR pos = :pos)
          AND (:grammarGroup IS NULL OR grammarGroup = :grammarGroup)
          AND (
                :query = '' OR
                word LIKE '%' || :query || '%' OR
                english LIKE '%' || :query || '%' OR
                germanMeaning LIKE '%' || :query || '%'
          )
        ORDER BY
          CASE WHEN :sortMode = 'ALPHA' THEN sortKey END ASC,
          CASE WHEN :sortMode = 'FREQUENCY' THEN frequencyRank END ASC,
          CASE WHEN :sortMode = 'CHAPTER' THEN level END ASC,
          CASE WHEN :sortMode = 'CHAPTER' THEN book END ASC,
          CASE WHEN :sortMode = 'CHAPTER' THEN chapter END ASC,
          id ASC
        """
    )
    fun observeFiltered(
        level: String?,
        book: String?,
        chapter: String?,
        pos: String?,
        grammarGroup: String?,
        query: String,
        sortMode: String
    ): Flow<List<VocabularyEntity>>

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

