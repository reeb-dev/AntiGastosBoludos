package com.antigastos.boludos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antigastos.boludos.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE period = :period LIMIT 1")
    fun observeForPeriod(period: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE period = :period LIMIT 1")
    suspend fun getForPeriod(period: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)
}
