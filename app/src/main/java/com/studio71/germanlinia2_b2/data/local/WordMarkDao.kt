package com.studio71.germanlinia2_b2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WordMarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mark: WordMarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WordMarkEntity>)

    @Query("DELETE FROM word_mark WHERE wordId = :wordId")
    suspend fun delete(wordId: String)

    @Query("DELETE FROM word_mark")
    suspend fun deleteAll()

    @Query("SELECT * FROM word_mark")
    suspend fun getAll(): List<WordMarkEntity>

    /** Live marker for a single word (null when unmarked). */
    @Query("SELECT marker FROM word_mark WHERE wordId = :wordId")
    fun observeMarker(wordId: String): Flow<Int?>

    /** All marks, used by the list to show a colored star per item. */
    @Query("SELECT * FROM word_mark")
    fun observeAll(): Flow<List<WordMarkEntity>>

    @Query("SELECT COUNT(*) FROM word_mark WHERE marker = :marker")
    fun observeCount(marker: Int): Flow<Int>
}

