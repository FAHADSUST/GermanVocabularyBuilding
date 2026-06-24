package com.studio71.germanlinia2_b2.data.local

import androidx.room.Entity

/**
 * One "seen" event per word and day (deduplicated by primary key).
 */
@Entity(tableName = "seen_word", primaryKeys = ["date", "wordId"])
data class SeenWordEntity(
    /** epoch day */
    val date: Long,
    val wordId: String,
    /** epoch millis when the word was first opened that day */
    val seenAtEpochMs: Long
)

