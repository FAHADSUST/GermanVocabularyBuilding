package com.studio71.germanlinia2_b2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WordImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WordImageEntity)

    @Query("SELECT * FROM word_image WHERE wordId = :wordId")
    suspend fun getById(wordId: String): WordImageEntity?

    @Query("SELECT * FROM word_image WHERE wordId = :wordId")
    fun observeById(wordId: String): Flow<WordImageEntity?>

    @Query(
        """
        SELECT v.id
        FROM vocabulary v
        LEFT JOIN word_image wi ON wi.wordId = v.id
        WHERE wi.wordId IS NULL OR wi.status = :pendingStatus
        ORDER BY v.orderIndex ASC
        LIMIT :limit
        """
    )
    suspend fun nextMissingWordIds(limit: Int, pendingStatus: String = WordImageEntity.STATUS_PENDING): List<String>
}

