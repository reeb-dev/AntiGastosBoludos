package com.antigastos.boludos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "copy_votes")
data class CopyVoteEntity(
    @PrimaryKey val templateId: String,
    val upVotes: Int = 0,
    val downVotes: Int = 0,
)
