package com.antigastos.boludos.data

import com.antigastos.boludos.data.local.AppDatabase
import com.antigastos.boludos.data.local.CategorySeed
import com.antigastos.boludos.data.local.dao.ExpenseListRow
import com.antigastos.boludos.data.local.entity.BudgetEntity
import com.antigastos.boludos.data.local.entity.CategoryEntity
import com.antigastos.boludos.data.local.entity.CopyVoteEntity
import com.antigastos.boludos.data.local.entity.ExpenseEntity
import com.antigastos.boludos.data.local.entity.GoalEntity
import com.antigastos.boludos.data.local.entity.PendingCrushEntity
import com.antigastos.boludos.data.local.entity.SubscriptionEntity
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.domain.CopyContext
import com.antigastos.boludos.domain.CopyEngine
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.PersonaCatalog
import com.antigastos.boludos.domain.RecurringPhantomDetector
import com.antigastos.boludos.domain.RecurringPhantomSuggestion
import com.antigastos.boludos.domain.ShameLevel
import com.antigastos.boludos.domain.SpanishMonths
import com.antigastos.boludos.domain.endExclusiveEpochMillis
import com.antigastos.boludos.domain.model.CategorySlice
import com.antigastos.boludos.domain.model.HomeUiModel
import com.antigastos.boludos.domain.model.StatsSnapshot
import com.antigastos.boludos.domain.model.TopCategorySpend
import com.antigastos.boludos.domain.model.TopDay
import com.antigastos.boludos.domain.startEpochMillis
import com.antigastos.boludos.domain.toPeriodString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class ExpenseRepository(
    private val db: AppDatabase,
    private val scoreRepository: ScoreRepository? = null,
) {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    @Volatile
    private var expensesChangedListener: (() -> Unit)? = null

    /** Hook para refrescar notificación persistente de racha, widgets, etc. */
    fun setExpensesChangedListener(listener: (() -> Unit)?) {
        expensesChangedListener = listener
    }

    private fun notifyExpensesChanged() {
        expensesChangedListener?.invoke()
    }

    /** Gasto en la semana calendario actual (lun→dom) para una categoría por slug. */
    suspend fun sumCategorySlugCalendarWeek(slug: String, weeksAgo: Int = 0): Long {
        if (slug.isBlank()) return 0L
        val (start, end) = weekRangeEpochMillis(weeksAgo)
        return db.expenseDao().sumByCategorySlugInRange(slug, start, end) ?: 0L
    }

    private fun weekRangeEpochMillis(weeksAgo: Int): Pair<Long, Long> {
        val today = LocalDate.now(zone)
        val monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val startMonday = monday.minusWeeks(weeksAgo.toLong())
        val start = startMonday.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = startMonday.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    /** Categoría del último gasto, o la primera del catálogo (para quick-add). */
    suspend fun quickExpenseDefaultCategoryId(): Long? =
        db.expenseDao().latestExpenseCategoryId()
            ?: db.categoryDao().getAll().firstOrNull()?.id

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

    /**
     * Snapshot puntual del `CopyContext` para el mes actual. Lo usa el chat de
     * personas y cualquier feature que necesite el contexto financiero del usuario
     * sin suscribirse al flow del Home.
     */
    suspend fun snapshotCopyContext(yearMonth: YearMonth = YearMonth.now()): CopyContext {
        val start = yearMonth.startEpochMillis(zone)
        val end = yearMonth.endExclusiveEpochMillis(zone)
        val prev = yearMonth.minusMonths(1)
        val prevStart = prev.startEpochMillis(zone)
        val prevEnd = prev.endExclusiveEpochMillis(zone)
        val period = yearMonth.toPeriodString()

        val rows = db.expenseDao().observeExpenseRows(start, end).first()
        val totalPesos = db.expenseDao().sumTotal(start, end) ?: 0L
        val prevTotalPesos = db.expenseDao().sumTotal(prevStart, prevEnd) ?: 0L
        val goal = db.goalDao().getByPeriodAndCategory(period, 0L)

        val bySlug = rows.groupBy { it.categorySlug }
        val totals = bySlug.mapValues { (_, list) -> list.sumOf { it.amountPesos } }
        val sorted = totals.entries.sortedByDescending { it.value }
        val topEntry = sorted.firstOrNull()
        val topName = topEntry?.let { (slug, _) ->
            rows.firstOrNull { it.categorySlug == slug }?.categoryName
        }
        val pctGoal = goal?.let {
            if (it.limitPesos > 0L) totalPesos.toDouble() / it.limitPesos.toDouble() else null
        }
        val today = LocalDate.now(zone)
        val isCurrentMonth = today.year == yearMonth.year && today.monthValue == yearMonth.monthValue
        val daySpentPesos = if (isCurrentMonth) {
            rows.filter { row ->
                val d = java.time.Instant.ofEpochMilli(row.occurredAt)
                    .atZone(zone)
                    .toLocalDate()
                d == today
            }.sumOf { it.amountPesos }
        } else {
            0L
        }
        val lastExpense = rows.firstOrNull()
        val deltaLucas = MoneyFormat.pesosToLucas(totalPesos) -
            MoneyFormat.pesosToLucas(prevTotalPesos)
        val lucasByCategory = totals.mapValues { (_, p) -> MoneyFormat.pesosToLucas(p) }

        return CopyContext(
            monthLabel = SpanishMonths.name(yearMonth.monthValue),
            totalLucas = MoneyFormat.pesosToLucas(totalPesos),
            topCategoryName = topName,
            topCategorySlug = topEntry?.key,
            topCategoryLucas = MoneyFormat.pesosToLucas(topEntry?.value ?: 0L),
            deltaLucasVsPrevMonth = if (rows.isEmpty() && prevTotalPesos == 0L) null else deltaLucas,
            pctOfMonthlyGoal = pctGoal,
            daySpentLucas = MoneyFormat.pesosToLucas(daySpentPesos),
            dayOfMonth = if (isCurrentMonth) today.dayOfMonth else 1,
            daysInMonth = yearMonth.lengthOfMonth(),
            expenseCount = rows.size,
            lastExpenseSlug = lastExpense?.categorySlug,
            lastExpenseLucas = lastExpense?.let { MoneyFormat.pesosToLucas(it.amountPesos) } ?: 0,
            lucasByCategory = lucasByCategory,
        )
    }

    fun observeHome(yearMonth: YearMonth): Flow<HomeUiModel> {
        val start = yearMonth.startEpochMillis(zone)
        val end = yearMonth.endExclusiveEpochMillis(zone)
        val prev = yearMonth.minusMonths(1)
        val prevStart = prev.startEpochMillis(zone)
        val prevEnd = prev.endExclusiveEpochMillis(zone)
        val period = yearMonth.toPeriodString()

        // Combinamos el set de votos con la fila de settings para conocer el persona elegido
        // sin pasar de los 5 args nominales que soporta `combine`.
        val votesAndSettings: Flow<Pair<List<CopyVoteEntity>, SettingsEntity>> = combine(
            db.copyVoteDao().observeAll(),
            db.settingsDao().observe(),
        ) { v, s -> v to (s ?: SettingsEntity()) }

        return combine(
            db.expenseDao().observeExpenseRows(start, end),
            db.expenseDao().observeTotalForRange(start, end),
            db.goalDao().observeForPeriod(period),
            db.expenseDao().observeTotalForRange(prevStart, prevEnd),
            votesAndSettings,
        ) { rows: List<ExpenseListRow>,
            totalOpt: Long?,
            goals: List<GoalEntity>,
            prevTotalOpt: Long?,
            votesAndSettingsPair: Pair<List<CopyVoteEntity>, SettingsEntity> ->
            val (votes, settings) = votesAndSettingsPair
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

            val today = LocalDate.now(zone)
            val lucasByCategory = totals.mapValues { (_, pesos) ->
                MoneyFormat.pesosToLucas(pesos)
            }
            val isCurrentMonth = today.year == yearMonth.year && today.monthValue == yearMonth.monthValue
            val rowDates = rows.map { row ->
                java.time.Instant.ofEpochMilli(row.occurredAt)
                    .atZone(zone)
                    .toLocalDate()
            }.toSet()
            val daySpentPesos = if (isCurrentMonth) {
                rows.filter { row ->
                    val d = java.time.Instant.ofEpochMilli(row.occurredAt)
                        .atZone(zone)
                        .toLocalDate()
                    d == today
                }.sumOf { it.amountPesos }
            } else {
                0L
            }
            val lastExpense = rows.firstOrNull()
            val dayOfMonth = if (isCurrentMonth) today.dayOfMonth else 1
            val daysInMonth = yearMonth.lengthOfMonth()
            val forgivenEpochs = settings.streakForgivenEpochDaysCsv
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapNotNull { it.toLongOrNull() }
                .toSet()
            val yesterday = today.minusDays(1)
            val yesterdayHadExpenses = isCurrentMonth &&
                yesterday.year == yearMonth.year &&
                yesterday.monthValue == yearMonth.monthValue &&
                rowDates.contains(yesterday)
            val streakDaysNoSpend = if (isCurrentMonth && daySpentPesos == 0L) {
                var d = today
                var streak = 0
                while (streak < daysInMonth) {
                    if (rowDates.contains(d) && d.toEpochDay() !in forgivenEpochs) break
                    streak += 1
                    d = d.minusDays(1)
                    if (d.monthValue != yearMonth.monthValue) break
                }
                streak
            } else {
                0
            }

            val byDayOfMonth: Map<Int, Long> = rows.groupBy { row ->
                java.time.Instant.ofEpochMilli(row.occurredAt)
                    .atZone(zone)
                    .toLocalDate().dayOfMonth
            }.mapValues { (_, list) -> list.sumOf { it.amountPesos } }

            val ctx = CopyContext(
                monthLabel = SpanishMonths.name(yearMonth.monthValue),
                totalLucas = MoneyFormat.pesosToLucas(totalPesos),
                topCategoryName = topName,
                topCategorySlug = topEntry?.key,
                topCategoryLucas = MoneyFormat.pesosToLucas(topEntry?.value ?: 0L),
                deltaLucasVsPrevMonth = if (rows.isEmpty() && prevTotalPesos == 0L) null else deltaLucas,
                pctOfMonthlyGoal = pctGoal,
                daySpentLucas = MoneyFormat.pesosToLucas(daySpentPesos),
                dayOfMonth = dayOfMonth,
                daysInMonth = daysInMonth,
                expenseCount = rows.size,
                lastExpenseSlug = lastExpense?.categorySlug,
                lastExpenseLucas = lastExpense?.let { MoneyFormat.pesosToLucas(it.amountPesos) } ?: 0,
                lucasByCategory = lucasByCategory,
            )

            val weights = votes.associate { v ->
                v.templateId to CopyEngine.weightForVotes(v.upVotes, v.downVotes)
            }
            val choice = CopyEngine.pickWithPersona(ctx, weights, settings.personaKey)
            val voteForChoice = votes.firstOrNull { it.templateId == choice.templateId }
            val persona = PersonaCatalog.byKey(settings.personaKey)
            val shame = ShameLevel.from(
                pctOfMonthlyGoal = pctGoal,
                dayOfMonth = dayOfMonth,
                lucasByCategory = lucasByCategory,
                totalLucas = MoneyFormat.pesosToLucas(totalPesos),
            )

            HomeUiModel(
                yearMonth = yearMonth,
                totalPesos = totalPesos,
                topCategories = topThree,
                globalGoalPesos = globalGoal,
                globalProgress = progress,
                personalityMessage = choice.text,
                personalityTemplateId = choice.templateId,
                personalityMood = choice.mood,
                personalityUpVotes = voteForChoice?.upVotes ?: 0,
                personalityDownVotes = voteForChoice?.downVotes ?: 0,
                expenseCount = rows.size,
                lastExpenseSlug = lastExpense?.categorySlug,
                daySpentPesos = daySpentPesos,
                streakDaysNoSpend = streakDaysNoSpend,
                shameLevel = shame,
                personaKey = persona.key,
                personaEmoji = persona.emoji,
                yesterdayHadExpenses = yesterdayHadExpenses,
                dailySpendByDay = byDayOfMonth,
            )
        }
    }

    suspend fun voteCopyUp(templateId: String) {
        if (templateId.isBlank() || templateId == "fallback") return
        db.copyVoteDao().voteUp(templateId)
    }

    suspend fun voteCopyDown(templateId: String) {
        if (templateId.isBlank() || templateId == "fallback") return
        db.copyVoteDao().voteDown(templateId)
    }

    suspend fun insertExpense(
        amountPesos: Long,
        categoryId: Long,
        note: String?,
        occurredAt: Long,
        sharedSplitPercent: Int? = null,
    ) {
        val now = System.currentTimeMillis()
        val id = db.expenseDao().insert(
            ExpenseEntity(
                amountPesos = amountPesos,
                categoryId = categoryId,
                note = note?.takeIf { it.isNotBlank() },
                occurredAt = occurredAt,
                updatedAtMillis = now,
                sharedSplitPercent = sharedSplitPercent?.coerceIn(0, 100),
            ),
        )
        val saved = db.expenseDao().getById(id) ?: return
        com.antigastos.boludos.domain.ScoreEngine.evaluateExpense(
            com.antigastos.boludos.domain.MoneyFormat.pesosToLucas(amountPesos).toLong(),
        )?.let { delta ->
            scoreRepository?.applyDelta(delta)
        }
        notifyExpensesChanged()
    }

    private companion object {
        private const val CRUSH_DELAY_MS: Long = 24L * 60L * 60L * 1000L
    }

    suspend fun insertPendingCrush(
        amountPesos: Long,
        categoryId: Long,
        note: String?,
    ): Long {
        val now = System.currentTimeMillis()
        val row = PendingCrushEntity(
            amountPesos = amountPesos,
            categoryId = categoryId,
            note = note?.takeIf { it.isNotBlank() },
            createdAtMillis = now,
            remindAtMillis = now + CRUSH_DELAY_MS,
        )
        return db.pendingCrushDao().insert(row)
    }

    suspend fun getPendingCrush(id: Long) = db.pendingCrushDao().getById(id)

    suspend fun deletePendingCrush(id: Long) {
        db.pendingCrushDao().deleteById(id)
    }

    suspend fun updateExpense(
        id: Long,
        amountPesos: Long,
        categoryId: Long,
        note: String?,
        occurredAt: Long,
        sharedSplitPercent: Int? = null,
    ) {
        val existing = db.expenseDao().getById(id) ?: return
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            amountPesos = amountPesos,
            categoryId = categoryId,
            note = note?.takeIf { it.isNotBlank() },
            occurredAt = occurredAt,
            updatedAtMillis = now,
            sharedSplitPercent = sharedSplitPercent?.coerceIn(0, 100),
        )
        db.expenseDao().update(updated)
        notifyExpensesChanged()
    }

    /**
     * Total pendiente que la pareja le debe al usuario en el rango (pesos).
     * Si no hay gastos compartidos en el rango, devuelve 0.
     */
    suspend fun partnerOwedPesosInRange(start: Long, end: Long): Long =
        db.expenseDao().sumPartnerOwedInRange(start, end) ?: 0L

    /** Cantidad de gastos compartidos cargados en el rango. */
    suspend fun countSharedExpensesInRange(start: Long, end: Long): Long =
        db.expenseDao().countSharedInRange(start, end)

    suspend fun deleteExpense(id: Long) {
        val entity = db.expenseDao().getById(id) ?: return
        db.expenseDao().delete(entity)
        notifyExpensesChanged()
    }

    suspend fun getExpense(id: Long) = db.expenseDao().getById(id)

    /**
     * Detecta un patrón de gastos manuales repetidos (misma categoría + monto
     * parecido en 3+ meses) sin suscripción equivalente.
     */
    suspend fun detectRecurringPhantomSuggestion(): RecurringPhantomSuggestion? {
        val expenses = db.expenseDao().getAllRaw()
        val cats = db.categoryDao().getAll()
        val subs = db.subscriptionDao().getActive()
        return RecurringPhantomDetector.detect(
            expenses = expenses,
            categories = cats,
            activeSubscriptions = subs,
            zone = zone,
        )
    }

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

    suspend fun upsertCategory(entity: CategoryEntity): Long = db.categoryDao().upsert(entity)
    suspend fun deleteCategory(entity: CategoryEntity) = db.categoryDao().delete(entity)
    suspend fun getCategoryBySlug(slug: String): CategoryEntity? = db.categoryDao().getBySlug(slug)

    fun observeSubscriptions(): Flow<List<SubscriptionEntity>> = db.subscriptionDao().observeAll()
    suspend fun upsertSubscription(entity: SubscriptionEntity): Long = db.subscriptionDao().upsert(entity)
    suspend fun deleteSubscription(entity: SubscriptionEntity) = db.subscriptionDao().delete(entity)

    /**
     * Materializa los gastos de las suscripciones activas para el mes pasado,
     * marcando `lastSeededPeriod`. Idempotente.
     */
    suspend fun seedSubscriptionsForCurrentMonth() {
        val now = LocalDate.now(zone)
        val period = YearMonth.from(now).toPeriodString()
        val subs = db.subscriptionDao().getActive()
        var any = false
        for (sub in subs) {
            if (sub.lastSeededPeriod == period) continue
            val day = sub.dayOfMonth.coerceIn(1, 28)
            val date = LocalDate.of(now.year, now.monthValue, day)
            if (date.isAfter(now)) continue
            any = true
            val occurredAt = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val nowMillis = System.currentTimeMillis()
            // Para cuotas mostramos el progreso ("Tele 4/12") en la nota.
            val note = if (sub.installmentsTotal != null) {
                val cur = (sub.installmentsPaid + 1).coerceAtMost(sub.installmentsTotal)
                "${sub.name} (cuota $cur/${sub.installmentsTotal})"
            } else {
                "${sub.name} (suscripción)"
            }
            val newId = db.expenseDao().insert(
                ExpenseEntity(
                    amountPesos = sub.amountPesos,
                    categoryId = sub.categoryId,
                    note = note,
                    occurredAt = occurredAt,
                    updatedAtMillis = nowMillis,
                ),
            )
            db.subscriptionDao().markSeeded(sub.id, period)
            if (sub.installmentsTotal != null) {
                db.subscriptionDao().advanceInstallment(sub.id)
            }
        }
        if (any) notifyExpensesChanged()
    }

    suspend fun countExpensesByCategoryInMonth(slug: String, yearMonth: YearMonth): Long {
        val start = yearMonth.startEpochMillis(zone)
        val end = yearMonth.endExclusiveEpochMillis(zone)
        return db.expenseDao().countByCategorySlugInRange(slug, start, end)
    }

    suspend fun sumByCategoryNow(yearMonth: YearMonth): Map<String, Long> {
        val start = yearMonth.startEpochMillis(zone)
        val end = yearMonth.endExclusiveEpochMillis(zone)
        return db.expenseDao().sumByCategory(start, end).associate { it.slug to it.totalPesos }
    }

    /**
     * Histórico (lifetime) de gastos cargados.
     *  - Total de filas y sumatoria en pesos.
     *  - Hora del último gasto.
     *  - Conteos y sumas por categoría.
     */
    suspend fun lifetimeStats(): LifetimeStats {
        val rows = db.expenseDao().getAllRaw()
        if (rows.isEmpty()) return LifetimeStats()

        val cats = db.categoryDao().getAll().associateBy { it.id }
        val countsBySlug = mutableMapOf<String, Long>()
        val sumsBySlug = mutableMapOf<String, Long>()
        for (row in rows) {
            val slug = cats[row.categoryId]?.slug ?: continue
            countsBySlug[slug] = (countsBySlug[slug] ?: 0L) + 1L
            sumsBySlug[slug] = (sumsBySlug[slug] ?: 0L) + row.amountPesos
        }
        val last = rows.maxByOrNull { it.occurredAt }
        val lastHour = last?.let {
            java.time.Instant.ofEpochMilli(it.occurredAt)
                .atZone(zone)
                .hour
        }
        return LifetimeStats(
            totalCount = rows.size.toLong(),
            totalPesos = rows.sumOf { it.amountPesos },
            lastExpenseHour = lastHour,
            countByCategorySlug = countsBySlug,
            sumByCategorySlug = sumsBySlug,
        )
    }

    data class LifetimeStats(
        val totalCount: Long = 0L,
        val totalPesos: Long = 0L,
        val lastExpenseHour: Int? = null,
        val countByCategorySlug: Map<String, Long> = emptyMap(),
        val sumByCategorySlug: Map<String, Long> = emptyMap(),
    )

    suspend fun deleteEverything() {
        db.expenseDao().clearAll()
        db.categoryDao().clearAll()
        db.categoryDao().insertAll(CategorySeed.defaults)
    }

    fun observeBudget(yearMonth: YearMonth): Flow<BudgetEntity?> =
        db.budgetDao().observeForPeriod(yearMonth.toPeriodString())

    suspend fun upsertBudget(
        yearMonth: YearMonth,
        salaryPesos: Long,
        otherIncomePesos: Long,
        fixedExpensesPesos: Long,
    ) {
        db.budgetDao().upsert(
            BudgetEntity(
                period = yearMonth.toPeriodString(),
                salaryPesos = salaryPesos.coerceAtLeast(0L),
                otherIncomePesos = otherIncomePesos.coerceAtLeast(0L),
                fixedExpensesPesos = fixedExpensesPesos.coerceAtLeast(0L),
            ),
        )
    }

    fun observeStats(yearMonth: YearMonth): Flow<StatsSnapshot> {
        val start = yearMonth.startEpochMillis(zone)
        val end = yearMonth.endExclusiveEpochMillis(zone)
        val prev = yearMonth.minusMonths(1)
        val pStart = prev.startEpochMillis(zone)
        val pEnd = prev.endExclusiveEpochMillis(zone)
        val pastYm = yearMonth.minusMonths(3)
        val pastStart = pastYm.startEpochMillis(zone)
        val pastEnd = pastYm.endExclusiveEpochMillis(zone)

        return combine(
            db.expenseDao().observeExpenseRows(start, end),
            db.expenseDao().observeTotalForRange(start, end),
            db.expenseDao().observeTotalForRange(pStart, pEnd),
            db.expenseDao().observeTotalForRange(pastStart, pastEnd),
        ) { rows, totalOpt, prevTotalOpt, past3Opt ->
            val total = totalOpt ?: 0L
            val prevTotal = prevTotalOpt ?: 0L
            val past3Total = past3Opt ?: 0L
            val bySlug = rows.groupBy { it.categorySlug }
            val slices = bySlug.mapValues { (_, list) -> list.sumOf { it.amountPesos } }
                .entries
                .sortedByDescending { it.value }
                .map { (slug, pesos) ->
                    val label = rows.firstOrNull { it.categorySlug == slug }?.categoryName ?: slug
                    CategorySlice(label = label, slug = slug, amountPesos = pesos)
                }
            val byDay: Map<Int, Long> = rows.groupBy { row ->
                java.time.Instant.ofEpochMilli(row.occurredAt).atZone(zone).toLocalDate().dayOfMonth
            }.mapValues { (_, list) -> list.sumOf { it.amountPesos } }

            val topDaysList: List<TopDay> = byDay.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { (day, pesos) ->
                    TopDay(
                        date = java.time.LocalDate.of(yearMonth.year, yearMonth.monthValue, day),
                        amountPesos = pesos,
                    )
                }

            val pastLabel: String?
            val pastTotal: Long?
            val pastDiff: Long?
            if (past3Total > 0L) {
                pastLabel = "${SpanishMonths.name(pastYm.monthValue)} ${pastYm.year}"
                pastTotal = past3Total
                pastDiff = total - past3Total
            } else {
                pastLabel = null
                pastTotal = null
                pastDiff = null
            }

            StatsSnapshot(
                yearMonth = yearMonth,
                totalPesos = total,
                previousMonthTotalPesos = prevTotal,
                categorySlices = slices,
                dailyTotals = byDay,
                topDays = topDaysList,
                pastSelfLabel = pastLabel,
                pastSelfTotalPesos = pastTotal,
                pastSelfDiffPesos = pastDiff,
            )
        }
    }
}
