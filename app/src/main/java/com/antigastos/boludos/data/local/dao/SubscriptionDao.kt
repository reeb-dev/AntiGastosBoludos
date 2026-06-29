package com.antigastos.boludos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.antigastos.boludos.data.local.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY active DESC, name ASC")
    fun observeAll(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE active = 1")
    suspend fun getActive(): List<SubscriptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SubscriptionEntity): Long

    @Update
    suspend fun update(entity: SubscriptionEntity)

    @Delete
    suspend fun delete(entity: SubscriptionEntity)

    @Query("UPDATE subscriptions SET lastSeededPeriod = :period WHERE id = :id")
    suspend fun markSeeded(id: Long, period: String)

    /**
     * Avanza el contador de cuotas pagadas y, si igualó al total, desactiva
     * la suscripción para que no se vuelva a sembrar.
     */
    @Query(
        """
        UPDATE subscriptions
        SET installmentsPaid = installmentsPaid + 1,
            active = CASE
                WHEN installmentsTotal IS NOT NULL AND installmentsPaid + 1 >= installmentsTotal THEN 0
                ELSE active
            END
        WHERE id = :id
        """,
    )
    suspend fun advanceInstallment(id: Long)
}
