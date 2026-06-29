package com.antigastos.boludos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antigastos.boludos.data.local.entity.ScoreEntity
import com.antigastos.boludos.data.local.entity.ScoreEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Query("SELECT * FROM score WHERE id = 1 LIMIT 1")
    fun observe(): Flow<ScoreEntity?>

    @Query("SELECT * FROM score WHERE id = 1 LIMIT 1")
    suspend fun get(): ScoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ScoreEntity)
}

@Dao
interface ScoreEventDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ScoreEventEntity): Long

    @Query("SELECT * FROM score_events ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 30): Flow<List<ScoreEventEntity>>

    @Query("SELECT * FROM score_events ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 30): List<ScoreEventEntity>

    @Query("SELECT COUNT(*) FROM score_events WHERE kind = :kind")
    suspend fun countByKind(kind: String): Long

    @Query("DELETE FROM score_events")
    suspend fun clearAll()
}
