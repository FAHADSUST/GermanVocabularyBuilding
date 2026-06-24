package com.studio71.germanlinia2_b2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Lightweight projection for the "seen words" list in statistics. */
data class SeenWordItem(
    val wordId: String,
    val displayWord: String,
    val english: String,
    val seenAtEpochMs: Long
)

@Dao
interface SeenWordDao {

    /**
     * @return row id, or -1 when this (date, wordId) pair already exists.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: SeenWordEntity): Long

    @Query(
        """
        SELECT
            s.wordId AS wordId,
            CASE
                WHEN v.article = '' THEN v.word
                ELSE (v.article || ' ' || v.word)
            END AS displayWord,
            v.english AS english,
            s.seenAtEpochMs AS seenAtEpochMs
        FROM seen_word s
        INNER JOIN vocabulary v ON v.id = s.wordId
        WHERE s.date = :date
        ORDER BY s.seenAtEpochMs DESC, v.sortKey ASC
        """
    )
    fun observeByDate(date: Long): Flow<List<SeenWordItem>>

    @Query("SELECT DISTINCT date FROM seen_word ORDER BY date DESC")
    fun observeSeenDates(): Flow<List<Long>>

    @Query("SELECT COUNT(DISTINCT wordId) FROM seen_word")
    fun observeDistinctWordCount(): Flow<Int>
}

