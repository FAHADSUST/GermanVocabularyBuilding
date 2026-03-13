package com.studio71.germanlinia2_b2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Personal spaced-repetition progress for a single word.
 * Dates are stored as epoch-day (Long) for easy comparison.
 * Review schedule (days from first learned): 1 -> 2 -> 3 -> 7 -> 15 -> 30.
 */
@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val wordId: String,
    /** epoch day the word was first studied */
    val firstLearned: Long,
    /** epoch day of the last review, or null if never reviewed after learning */
    val lastReviewed: Long? = null,
    /** epoch day when the word is next due */
    val nextDue: Long,
    /** index into the interval schedule (0 = first interval) */
    val intervalIndex: Int = 0,
    /** Leitner-style box; higher = better known */
    val box: Int = 1
)

