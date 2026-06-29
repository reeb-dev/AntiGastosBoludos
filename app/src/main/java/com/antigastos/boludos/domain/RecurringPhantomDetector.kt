package com.antigastos.boludos.domain

import com.antigastos.boludos.data.local.entity.CategoryEntity
import com.antigastos.boludos.data.local.entity.ExpenseEntity
import com.antigastos.boludos.data.local.entity.SubscriptionEntity
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs

/**
 * Detecta gastos cargados manualmente que se repiten mes a mes (misma categoría
 * y monto similar) y podrían ser una [SubscriptionEntity].
 */
object RecurringPhantomDetector {

    private const val BUCKET_PESOS: Long = 2_500L
    private const val MONTHS_WINDOW: Long = 4L
    private const val MIN_DISTINCT_MONTHS: Int = 3
    private const val MIN_HITS: Int = 3
    private const val AMOUNT_TOLERANCE_PERCENT: Long = 20L

    private data class GroupKey(val categoryId: Long, val amountBucket: Long)

    private fun isLikelyManualEntry(note: String?): Boolean {
        val n = note?.lowercase().orEmpty()
        if ("(suscripción)" in n) return false
        if ("(cuota" in n) return false
        return true
    }

    private fun bucketAmount(amountPesos: Long): Long =
        (amountPesos / BUCKET_PESOS) * BUCKET_PESOS

    private fun hasSimilarActiveSubscription(
        categoryId: Long,
        typicalAmount: Long,
        subs: List<SubscriptionEntity>,
    ): Boolean {
        if (typicalAmount <= 0L) return true
        return subs.any { sub ->
            sub.active &&
                sub.installmentsTotal == null &&
                sub.categoryId == categoryId &&
                abs(sub.amountPesos - typicalAmount) * 100L <= typicalAmount * AMOUNT_TOLERANCE_PERCENT
        }
    }

    /**
     * Analiza gastos en los últimos [MONTHS_WINDOW] meses calendario (incluye el actual).
     */
    fun detect(
        expenses: List<ExpenseEntity>,
        categories: List<CategoryEntity>,
        activeSubscriptions: List<SubscriptionEntity>,
        zone: ZoneId,
        nowYm: YearMonth = YearMonth.now(zone),
    ): RecurringPhantomSuggestion? {
        val catById = categories.associateBy { it.id }
        val windowStart = nowYm.minusMonths(MONTHS_WINDOW - 1).atDay(1)
        val startMs = windowStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = nowYm.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val filtered = expenses.asSequence()
            .filter { it.occurredAt >= startMs && it.occurredAt < endMs }
            .filter { isLikelyManualEntry(it.note) }
            .filter { it.amountPesos > 0L }
            .toList()

        if (filtered.size < MIN_HITS) return null

        val groups = filtered.groupBy { e ->
            GroupKey(e.categoryId, bucketAmount(e.amountPesos))
        }

        var best: RecurringPhantomSuggestion? = null
        for ((key, list) in groups) {
            if (list.size < MIN_HITS) continue
            val months = list.map { e ->
                YearMonth.from(Instant.ofEpochMilli(e.occurredAt).atZone(zone).toLocalDate())
            }.toSet()
            if (months.size < MIN_DISTINCT_MONTHS) continue

            val amounts = list.map { it.amountPesos }.sorted()
            val median = amounts[amounts.size / 2]
            if (hasSimilarActiveSubscription(key.categoryId, median, activeSubscriptions)) continue

            val cat = catById[key.categoryId] ?: continue
            val nameGuess = list.mapNotNull { it.note?.trim()?.takeIf { n -> n.isNotBlank() } }
                .maxByOrNull { it.length }
                ?.take(40)
                ?: cat.name

            val typicalDay = list.map { e ->
                Instant.ofEpochMilli(e.occurredAt).atZone(zone).dayOfMonth
            }.sorted()[list.size / 2].coerceIn(1, 28)

            val suggestionKey = "${key.categoryId}_${key.amountBucket}"
            val cand = RecurringPhantomSuggestion(
                suggestionKey = suggestionKey,
                categoryId = key.categoryId,
                categoryName = cat.name,
                suggestedName = nameGuess,
                typicalAmountPesos = median,
                distinctMonths = months.size,
                hitCount = list.size,
                suggestedDayOfMonth = typicalDay,
            )
            val better = when {
                best == null -> true
                cand.distinctMonths > best.distinctMonths -> true
                cand.distinctMonths == best.distinctMonths && cand.hitCount > best.hitCount -> true
                else -> false
            }
            if (better) best = cand
        }
        return best
    }
}
