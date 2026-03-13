package com.studio71.germanlinia2_b2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Aggregated daily learning stats, used to draw the bar chart and compute streaks.
 */
@Entity(tableName = "daily_stat")
data class DailyStatEntity(
    /** epoch day */
    @PrimaryKey val date: Long,
    val wordsLearned: Int = 0,
    val wordsReviewed: Int = 0
)

