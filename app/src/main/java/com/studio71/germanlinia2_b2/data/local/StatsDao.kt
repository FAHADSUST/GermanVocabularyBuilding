package com.studio71.germanlinia2_b2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: DailyStatEntity)

    @Query("SELECT * FROM daily_stat WHERE date = :date")
    suspend fun getByDate(date: Long): DailyStatEntity?

    @Query("SELECT * FROM daily_stat ORDER BY date ASC")
    fun observeAll(): Flow<List<DailyStatEntity>>

    @Query("SELECT * FROM daily_stat WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeRange(from: Long, to: Long): Flow<List<DailyStatEntity>>
}

