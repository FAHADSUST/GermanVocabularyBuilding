package com.studio71.germanlinia2_b2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_game")
data class ActiveGameEntity(
    @PrimaryKey val id: String,
    val mode: String,
    val createdAtEpochMs: Long,
    val wordIds: String,
    val progressIndex: Int,
    val knownCount: Int,
    val againCount: Int,
    val extraData: String = ""
)

