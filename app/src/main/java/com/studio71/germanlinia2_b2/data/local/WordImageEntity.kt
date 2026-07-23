package com.studio71.germanlinia2_b2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One optional meaning image per word.
 * The URL points to an online image; image bytes are cached by Coil.
 */
@Entity(tableName = "word_image")
data class WordImageEntity(
    @PrimaryKey val wordId: String,
    val imageUrl: String? = null,
    val query: String = "",
    val status: String = STATUS_PENDING,
    val lastTriedAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_LOADING = "LOADING"
        const val STATUS_READY = "READY"
        const val STATUS_NO_RESULT = "NO_RESULT"
        const val STATUS_ERROR = "ERROR"
    }
}

