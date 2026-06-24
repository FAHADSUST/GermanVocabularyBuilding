package com.studio71.germanlinia2_b2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User-authored note for one vocabulary item.
 */
@Entity(tableName = "word_comment")
data class WordCommentEntity(
    @PrimaryKey val wordId: String,
    val comment: String,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

