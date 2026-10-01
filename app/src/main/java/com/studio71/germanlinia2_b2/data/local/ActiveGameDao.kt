package com.studio71.germanlinia2_b2.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveGameDao {

    @Query("SELECT * FROM active_game ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<ActiveGameEntity>>

    @Query("SELECT * FROM active_game WHERE id = :id")
    suspend fun getById(id: String): ActiveGameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(game: ActiveGameEntity)

    @Query("DELETE FROM active_game WHERE id = :id")
    suspend fun delete(id: String)
}

