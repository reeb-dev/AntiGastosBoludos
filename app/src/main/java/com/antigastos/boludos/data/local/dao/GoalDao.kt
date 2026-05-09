package com.antigastos.boludos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antigastos.boludos.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE period = :period")
    fun observeForPeriod(period: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE period = :period AND categoryId = :categoryId")
    suspend fun getByPeriodAndCategory(period: String, categoryId: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long
}
