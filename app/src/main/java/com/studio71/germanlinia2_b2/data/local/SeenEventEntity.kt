package com.studio71.germanlinia2_b2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Append-only history of every word-open event.
 * Used for "last 50 events" review and optional cloud sync.
 */
@Entity(
    tableName = "seen_event",
    indices = [
        Index(value = ["wordId"]),
        Index(value = ["seenAtEpochMs"])
    ]
)
data class SeenEventEntity(
    @PrimaryKey val eventId: String,
    val wordId: String,
    val seenAtEpochMs: Long
)

