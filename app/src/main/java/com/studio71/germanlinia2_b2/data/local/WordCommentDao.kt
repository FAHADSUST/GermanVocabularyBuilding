package com.studio71.germanlinia2_b2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WordCommentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(comment: WordCommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WordCommentEntity>)

    @Query("DELETE FROM word_comment WHERE wordId = :wordId")
    suspend fun delete(wordId: String)

    @Query("DELETE FROM word_comment")
    suspend fun deleteAll()

    @Query("SELECT * FROM word_comment")
    suspend fun getAll(): List<WordCommentEntity>

    @Query("SELECT comment FROM word_comment WHERE wordId = :wordId")
    fun observeComment(wordId: String): Flow<String?>
}

