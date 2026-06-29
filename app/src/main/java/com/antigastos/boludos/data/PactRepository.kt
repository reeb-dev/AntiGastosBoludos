package com.antigastos.boludos.data

import com.antigastos.boludos.data.local.AppDatabase
import com.antigastos.boludos.data.local.entity.PactEntity
import kotlinx.coroutines.flow.Flow

class PactRepository(private val db: AppDatabase) {

    fun observe(): Flow<List<PactEntity>> = db.pactDao().observeAll()

    suspend fun activeNow(): List<PactEntity> = db.pactDao().activeNow()

    suspend fun create(
        categorySlug: String,
        days: Int,
        friendName: String? = null,
        stakeText: String? = null,
    ): Long {
        val now = System.currentTimeMillis()
        val end = now + days.coerceAtLeast(1).toLong() * 24L * 3600L * 1000L
        return db.pactDao().upsert(
            PactEntity(
                categorySlug = categorySlug,
                startEpochMillis = now,
                endEpochMillis = end,
                friendName = friendName?.trim()?.takeIf { it.isNotBlank() },
                stakeText = stakeText?.trim()?.takeIf { it.isNotBlank() },
            ),
        )
    }

    /** Llamar cuando se inserta un gasto: si rompe un pacto activo, lo marca. */
    suspend fun onExpenseInserted(categorySlug: String, occurredAt: Long): List<Long> {
        val broken = mutableListOf<Long>()
        val active = activeNow()
        for (p in active) {
            if (p.categorySlug == categorySlug && occurredAt in p.startEpochMillis..p.endEpochMillis) {
                db.pactDao().markBroken(p.id, occurredAt)
                broken += p.id
            }
        }
        return broken
    }

    /** Marca como completos los pactos cuyo `endEpochMillis` ya pasó y no se rompieron. */
    suspend fun completeExpired(now: Long = System.currentTimeMillis()): List<Long> {
        val completed = mutableListOf<Long>()
        for (p in activeNow()) {
            if (p.endEpochMillis <= now) {
                db.pactDao().markCompleted(p.id, now)
                completed += p.id
            }
        }
        return completed
    }
}
