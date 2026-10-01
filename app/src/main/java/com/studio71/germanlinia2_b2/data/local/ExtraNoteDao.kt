package com.studio71.germanlinia2_b2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtraNoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: ExtraNoteEntity)

    @Query("DELETE FROM extra_notes WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("SELECT * FROM extra_notes ORDER BY updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<ExtraNoteEntity>>

    @Query("SELECT * FROM extra_notes WHERE id = :id")
    suspend fun getById(id: Int): ExtraNoteEntity?
}

