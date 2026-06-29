package com.antigastos.boludos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antigastos.boludos.data.local.entity.PendingCrushEntity

@Dao
interface PendingCrushDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: PendingCrushEntity): Long

    @Query("SELECT * FROM pending_crushes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PendingCrushEntity?

    @Query("DELETE FROM pending_crushes WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
