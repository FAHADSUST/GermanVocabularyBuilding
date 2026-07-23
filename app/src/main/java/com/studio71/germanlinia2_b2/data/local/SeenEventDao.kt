package com.studio71.germanlinia2_b2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Projection for recent seen-event review rows. */
data class SeenEventItem(
    val eventId: String,
    val wordId: String,
    val displayWord: String,
    val english: String,
    val seenAtEpochMs: Long
)

@Dao
interface SeenEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: SeenEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SeenEventEntity>)

    @Query("SELECT * FROM seen_event")
    suspend fun getAll(): List<SeenEventEntity>

    @Query("DELETE FROM seen_event")
    suspend fun deleteAll()

    @Query(
        """
        SELECT
            s.eventId AS eventId,
            s.wordId AS wordId,
            CASE
                WHEN v.article = '' THEN v.word
                ELSE (v.article || ' ' || v.word)
            END AS displayWord,
            v.english AS english,
            s.seenAtEpochMs AS seenAtEpochMs
        FROM seen_event s
        INNER JOIN vocabulary v ON v.id = s.wordId
        ORDER BY s.seenAtEpochMs DESC, v.sortKey ASC
        LIMIT :limit
        """
    )
    suspend fun getRecent(limit: Int): List<SeenEventItem>
}

