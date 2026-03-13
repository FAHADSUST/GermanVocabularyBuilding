package com.studio71.germanlinia2_b2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ProgressEntity)

    @Query("SELECT * FROM progress WHERE wordId = :wordId")
    suspend fun getById(wordId: String): ProgressEntity?

    @Query("SELECT * FROM progress WHERE wordId = :wordId")
    fun observeById(wordId: String): Flow<ProgressEntity?>

    /** All words due for review on or before [day] (epoch day). */
    @Query(
        """
        SELECT v.* FROM vocabulary v
        INNER JOIN progress p ON p.wordId = v.id
        WHERE p.nextDue <= :day
        ORDER BY p.nextDue ASC, v.sortKey ASC
        """
    )
    fun observeDue(day: Long): Flow<List<VocabularyEntity>>

    @Query("SELECT COUNT(*) FROM progress WHERE nextDue <= :day")
    fun observeDueCount(day: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM progress")
    fun observeLearnedCount(): Flow<Int>
}

