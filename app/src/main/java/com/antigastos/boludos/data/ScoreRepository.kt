package com.antigastos.boludos.data

import com.antigastos.boludos.data.local.AppDatabase
import com.antigastos.boludos.data.local.entity.ScoreEntity
import com.antigastos.boludos.data.local.entity.ScoreEventEntity
import com.antigastos.boludos.domain.ScoreEngine
import com.antigastos.boludos.domain.model.HomeUiModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

/**
 * Puntos y racha en Room (solo este dispositivo).
 */
class ScoreRepository(
    private val db: AppDatabase,
) {

    fun observeScore(): Flow<ScoreEntity?> = db.scoreDao().observe()

    fun observeRecentEvents(limit: Int = 30): Flow<List<ScoreEventEntity>> =
        db.scoreEventDao().observeRecent(limit)

    suspend fun snapshot(): ScoreEntity =
        db.scoreDao().get() ?: ScoreEntity().also { db.scoreDao().upsert(it) }

    suspend fun applyDelta(delta: ScoreEngine.Delta) {
        val updated = applyDeltaInternal(snapshot(), delta)
        db.scoreDao().upsert(updated)
        recordEvents(listOf(delta))
    }

    /**
     * Evaluación diaria a partir del Home. Devuelve los deltas aplicados
     * (lista vacía si ya se había evaluado este día con la misma firma).
     */
    suspend fun evaluateDay(home: HomeUiModel): List<ScoreEngine.Delta> {
        val today = LocalDate.now().toString()
        val sig = daySignature(home)
        val current = snapshot()
        if (current.lastEvaluatedDay == today && current.lastEvaluatedSig == sig) {
            return emptyList()
        }
        val deltas = ScoreEngine.evaluateDay(home)
        var updated = current
        for (d in deltas) {
            updated = applyDeltaInternal(updated, d)
        }
        updated = updated.copy(lastEvaluatedDay = today, lastEvaluatedSig = sig)
        db.scoreDao().upsert(updated)
        if (deltas.isNotEmpty()) {
            recordEvents(deltas)
        }
        return deltas
    }

    private fun daySignature(home: HomeUiModel): String =
        buildString {
            append(home.yearMonth)
            append('|')
            append(home.expenseCount)
            append('|')
            append(home.daySpentPesos)
            append('|')
            append(home.streakDaysNoSpend)
            append('|')
            append(home.globalProgress ?: -1)
        }

    private fun applyDeltaInternal(score: ScoreEntity, delta: ScoreEngine.Delta): ScoreEntity {
        val ym = YearMonth.now().toString()
        var monthPts = score.monthPoints
        var currentMonth = score.currentMonth
        if (currentMonth != ym) {
            currentMonth = ym
            monthPts = 0L
        }
        val newPoints = (score.points + delta.points).coerceAtLeast(0L)
        monthPts = (monthPts + delta.points).coerceAtLeast(0L)
        val best = maxOf(score.bestMonthPoints, monthPts)
        return score.copy(
            points = newPoints,
            currentMonth = currentMonth,
            monthPoints = monthPts,
            bestMonthPoints = best,
            rewardsCount = score.rewardsCount +
                if (delta.kind == ScoreEngine.Kind.REWARD) 1 else 0,
            failsCount = score.failsCount +
                if (delta.kind == ScoreEngine.Kind.FAIL) 1 else 0,
        )
    }

    private suspend fun recordEvents(deltas: List<ScoreEngine.Delta>) {
        val day = LocalDate.now().toString()
        val now = System.currentTimeMillis()
        for (d in deltas) {
            db.scoreEventDao().insert(
                ScoreEventEntity(
                    kind = d.kind.storage,
                    label = d.label,
                    emoji = d.emoji,
                    delta = d.points,
                    day = day,
                    createdAt = now,
                ),
            )
        }
    }
}
