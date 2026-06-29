package com.antigastos.boludos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.antigastos.boludos.data.local.entity.CopyVoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CopyVoteDao {
    @Query("SELECT * FROM copy_votes")
    fun observeAll(): Flow<List<CopyVoteEntity>>

    @Query("SELECT * FROM copy_votes WHERE templateId = :id")
    suspend fun get(id: String): CopyVoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CopyVoteEntity)

    @Transaction
    suspend fun voteUp(id: String) {
        val current = get(id) ?: CopyVoteEntity(templateId = id)
        upsert(current.copy(upVotes = current.upVotes + 1))
    }

    @Transaction
    suspend fun voteDown(id: String) {
        val current = get(id) ?: CopyVoteEntity(templateId = id)
        upsert(current.copy(downVotes = current.downVotes + 1))
    }
}
