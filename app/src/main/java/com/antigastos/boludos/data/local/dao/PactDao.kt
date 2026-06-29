package com.antigastos.boludos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antigastos.boludos.data.local.entity.PactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PactDao {
    @Query("SELECT * FROM pacts ORDER BY id DESC")
    fun observeAll(): Flow<List<PactEntity>>

    @Query("SELECT * FROM pacts WHERE brokenAt IS NULL AND completedAt IS NULL")
    suspend fun activeNow(): List<PactEntity>

    @Query("SELECT * FROM pacts WHERE id = :id")
    suspend fun getById(id: Long): PactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PactEntity): Long

    @Query("UPDATE pacts SET brokenAt = :ts WHERE id = :id")
    suspend fun markBroken(id: Long, ts: Long)

    @Query("UPDATE pacts SET completedAt = :ts WHERE id = :id")
    suspend fun markCompleted(id: Long, ts: Long)
}
