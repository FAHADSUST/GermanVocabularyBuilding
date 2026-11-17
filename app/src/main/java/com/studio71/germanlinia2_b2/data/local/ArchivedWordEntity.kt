package com.studio71.germanlinia2_b2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A word that left the active review plan, either because the plan was reset or because
 * the word passed its final review. Keeping it here means the list is never lost and a
 * word can be taken back into the plan later.
 */
@Entity(tableName = "archived_word")
data class ArchivedWordEntity(
    @PrimaryKey val wordId: String,
    /** epoch millis when the word was archived */
    val archivedAtEpochMs: Long,
    /** [REASON_RESET] or [REASON_MASTERED] */
    val reason: String
) {
    companion object {
        const val REASON_RESET = "RESET"
        const val REASON_MASTERED = "MASTERED"
    }
}
