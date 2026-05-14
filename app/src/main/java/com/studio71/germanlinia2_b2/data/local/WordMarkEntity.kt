package com.studio71.germanlinia2_b2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-applied difficulty marker for a single word, independent of the SRS
 * progress in [ProgressEntity]. Lets the learner flag words to revise later and
 * filter the list by how hard they are to remember.
 */
@Entity(tableName = "word_mark")
data class WordMarkEntity(
    @PrimaryKey val wordId: String,
    /** 1 = hard (red), 2 = medium (yellow), 3 = easy (blue). */
    val marker: Int
)

/**
 * Difficulty marker shown as a colored star.
 *  - [HARD]   red star    – hard to remember
 *  - [MEDIUM] yellow star – medium
 *  - [EASY]   blue star   – easy
 */
enum class WordMarker(val value: Int) {
    NONE(0),
    HARD(1),
    MEDIUM(2),
    EASY(3);

    companion object {
        fun fromValue(value: Int?): WordMarker =
            entries.firstOrNull { it.value == value } ?: NONE
    }
}

