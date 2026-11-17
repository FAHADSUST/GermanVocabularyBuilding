package com.studio71.germanlinia2_b2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Lightweight projection for the archive list. */
data class ArchivedWordItem(
    val wordId: String,
    val displayWord: String,
    val english: String,
    val archivedAtEpochMs: Long,
    val reason: String
)

@Dao
interface ArchivedWordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ArchivedWordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<ArchivedWordEntity>)

    @Query("DELETE FROM archived_word WHERE wordId = :wordId")
    suspend fun delete(wordId: String)

    /** Drops the archive entries of words that are back in the review plan. */
    @Query("DELETE FROM archived_word WHERE wordId IN (SELECT wordId FROM progress)")
    suspend fun deleteRestored()

    /** True when the word passed its final review (a reset does not count). */
    @Query(
        "SELECT EXISTS(SELECT 1 FROM archived_word WHERE wordId = :wordId AND reason = '${ArchivedWordEntity.REASON_MASTERED}')"
    )
    suspend fun isMastered(wordId: String): Boolean

    /** Archived words that are not in the review plan right now, newest first. */
    @Query(
        """
        SELECT
            a.wordId AS wordId,
            CASE
                WHEN v.article = '' THEN v.word
                ELSE (v.article || ' ' || v.word)
            END AS displayWord,
            v.english AS english,
            a.archivedAtEpochMs AS archivedAtEpochMs,
            a.reason AS reason
        FROM archived_word a
        INNER JOIN vocabulary v ON v.id = a.wordId
        WHERE a.wordId NOT IN (SELECT wordId FROM progress)
        ORDER BY a.archivedAtEpochMs DESC, v.sortKey ASC
        """
    )
    fun observeAll(): Flow<List<ArchivedWordItem>>

    @Query(
        """
        SELECT COUNT(*) FROM archived_word a
        INNER JOIN vocabulary v ON v.id = a.wordId
        WHERE a.wordId NOT IN (SELECT wordId FROM progress)
        """
    )
    fun observeCount(): Flow<Int>

    @Query(
        """
        SELECT a.wordId FROM archived_word a
        INNER JOIN vocabulary v ON v.id = a.wordId
        WHERE a.wordId NOT IN (SELECT wordId FROM progress)
        """
    )
    suspend fun getArchivedWordIds(): List<String>
}
