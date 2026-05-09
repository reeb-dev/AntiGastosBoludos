package com.antigastos.boludos.data

import com.antigastos.boludos.data.local.AppDatabase
import com.antigastos.boludos.data.local.CategorySeed
import com.antigastos.boludos.data.local.dao.ExpenseListRow
import com.antigastos.boludos.data.local.entity.ExpenseEntity
import com.antigastos.boludos.data.local.entity.GoalEntity
import com.antigastos.boludos.domain.CopyContext
import com.antigastos.boludos.domain.CopyEngine
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.SpanishMonths
import com.antigastos.boludos.domain.endExclusiveEpochMillis
import com.antigastos.boludos.domain.model.CategorySlice
import com.antigastos.boludos.domain.model.HomeUiModel
import com.antigastos.boludos.domain.model.StatsSnapshot
import com.antigastos.boludos.domain.model.TopCategorySpend
import com.antigastos.boludos.domain.startEpochMillis
import com.antigastos.boludos.domain.toPeriodString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth
import java.time.ZoneId

class ExpenseRepository(private val db: AppDatabase) {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    suspend fun ensureCategoriesSeeded() {
        if (db.categoryDao().count() > 0L) return
        db.categoryDao().insertAll(CategorySeed.defaults)
    }

    fun observeCategories() = db.categoryDao().observeAll()

    suspend fun getCategories() = db.categoryDao().getAll()

    fun observeExpenseRows(yearMonth: YearMonth): Flow<List<ExpenseListRow>> {
        val start = yearMonth.startEpochMillis(zone)
        val end = yearMonth.endExclusiveEpochMillis(zone)
        return db.expenseDao().observeExpenseRows(start, end)
    }

    fun observeHome(yearMonth: YearMonth): Flow<HomeUiModel> {
        val start = yearMonth.startEpochMillis(zone)
        val end = yearMonth.endExclusiveEpochMillis(zone)
        val prev = yearMonth.minusMonths(1)
        val prevStart = prev.startEpochMillis(zone)
        val prevEnd = prev.endExclusiveEpochMillis(zone)
        val period = yearMonth.toPeriodString()

        return combine(
            db.expenseDao().observeExpenseRows(start, end),
            db.expenseDao().observeTotalForRange(start, end),
            db.goalDao().observeForPeriod(period),
            db.expenseDao().observeTotalForRange(prevStart, prevEnd),
        ) { rows, totalOpt, goals, prevTotalOpt ->
            val totalPesos = totalOpt ?: 0L
            val prevTotalPesos = prevTotalOpt ?: 0L

            val bySlug = rows.groupBy { it.categorySlug }
            val totals = bySlug.mapValues { (_, list) -> list.sumOf { it.amountPesos } }
            val sorted = totals.entries.sortedByDescending { it.value }
            val topThree = sorted.take(3).map { (slug, pesos) ->
                val label = rows.firstOrNull { it.categorySlug == slug }?.categoryName ?: slug
                TopCategorySpend(name = label, slug = slug, amountPesos = pesos)
            }

            val topEntry = sorted.firstOrNull()
            val topName = topEntry?.let { (slug, _) ->
                rows.firstOrNull { it.categorySlug == slug }?.categoryName
            }
            val globalGoal = goals.find { it.categoryId == 0L }?.limitPesos
            val progress = if (globalGoal != null && globalGoal > 0L) {
                (totalPesos.toFloat() / globalGoal.toFloat()).coerceIn(0f, 1.5f)
            } else {
                null
            }

            val deltaLucas = MoneyFormat.pesosToLucas(totalPesos) -
                MoneyFormat.pesosToLucas(prevTotalPesos)

            val pctGoal = if (globalGoal != null && globalGoal > 0L) {
                totalPesos.toDouble() / globalGoal.toDouble()
            } else {
                null
            }

            val ctx = CopyContext(
                monthLabel = SpanishMonths.name(yearMonth.monthValue),
                totalLucas = MoneyFormat.pesosToLucas(totalPesos),
                topCategoryName = topName,
                topCategorySlug = topEntry?.key,
                topCategoryLucas = MoneyFormat.pesosToLucas(topEntry?.value ?: 0L),
                deltaLucasVsPrevMonth = if (rows.isEmpty() && prevTotalPesos == 0L) null else deltaLucas,
                pctOfMonthlyGoal = pctGoal,
            )

            HomeUiModel(
                yearMonth = yearMonth,
                totalPesos = totalPesos,
                topCategories = topThree,
                globalGoalPesos = globalGoal,
                globalProgress = progress,
                personalityMessage = CopyEngine.pickMessage(ctx),
            )
        }
    }

    suspend fun insertExpense(amountPesos: Long, categoryId: Long, note: String?, occurredAt: Long) {
        db.expenseDao().insert(
            ExpenseEntity(
                amountPesos = amountPesos,
                categoryId = categoryId,
                note = note?.takeIf { it.isNotBlank() },
                occurredAt = occurredAt,
            ),
        )
    }

    suspend fun updateExpense(id: Long, amountPesos: Long, categoryId: Long, note: String?, occurredAt: Long) {
        db.expenseDao().update(
            ExpenseEntity(
                id = id,
                amountPesos = amountPesos,
                categoryId = categoryId,
                note = note?.takeIf { it.isNotBlank() },
                occurredAt = occurredAt,
            ),
        )
    }

    suspend fun deleteExpense(id: Long) {
        val entity = db.expenseDao().getById(id) ?: return
        db.expenseDao().delete(entity)
    }

    suspend fun getExpense(id: Long) = db.expenseDao().getById(id)

    fun observeGoals(yearMonth: YearMonth): Flow<List<GoalEntity>> =
        db.goalDao().observeForPeriod(yearMonth.toPeriodString())

    suspend fun upsertGoal(yearMonth: YearMonth, categoryId: Long, limitPesos: Long) {
        val period = yearMonth.toPeriodString()
        val existing = db.goalDao().getByPeriodAndCategory(period, categoryId)
        db.goalDao().insert(
            GoalEntity(
                id = existing?.id ?: 0L,
                categoryId = categoryId,
                period = period,
                limitPesos = limitPesos,
            ),
        )
    }

    fun observeStats(yearMonth: YearMonth): Flow<StatsSnapshot> {
        val start = yearMonth.startEpochMillis(zone)
        val end = yearMonth.endExclusiveEpochMillis(zone)
        val prev = yearMonth.minusMonths(1)
        val pStart = prev.startEpochMillis(zone)
        val pEnd = prev.endExclusiveEpochMillis(zone)

        return combine(
            db.expenseDao().observeExpenseRows(start, end),
            db.expenseDao().observeTotalForRange(start, end),
            db.expenseDao().observeTotalForRange(pStart, pEnd),
        ) { rows, totalOpt, prevTotalOpt ->
            val total = totalOpt ?: 0L
            val prevTotal = prevTotalOpt ?: 0L
            val bySlug = rows.groupBy { it.categorySlug }
            val slices = bySlug.mapValues { (_, list) -> list.sumOf { it.amountPesos } }
                .entries
                .sortedByDescending { it.value }
                .map { (slug, pesos) ->
                    val label = rows.firstOrNull { it.categorySlug == slug }?.categoryName ?: slug
                    CategorySlice(label = label, slug = slug, amountPesos = pesos)
                }
            StatsSnapshot(
                yearMonth = yearMonth,
                totalPesos = total,
                previousMonthTotalPesos = prevTotal,
                categorySlices = slices,
            )
        }
    }
}
