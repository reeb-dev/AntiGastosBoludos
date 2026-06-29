package com.antigastos.boludos.data

import com.antigastos.boludos.data.local.AppDatabase
import com.antigastos.boludos.data.local.entity.AchievementEntity
import com.antigastos.boludos.domain.AchievementMeta
import com.antigastos.boludos.domain.AchievementsCatalog
import com.antigastos.boludos.domain.HolidayCalendar
import com.antigastos.boludos.domain.RandomTrophyCatalog
import com.antigastos.boludos.domain.model.HomeUiModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class AchievementWithMeta(
    val key: String,
    val title: String,
    val description: String,
    val emoji: String,
    val unlockedAt: Long?,
    val category: String = "Generales",
)

class AchievementsRepository(private val db: AppDatabase) {

    fun observe(): Flow<List<AchievementEntity>> = db.achievementDao().observeAll()

    suspend fun unlock(key: String): Boolean {
        val existing = db.achievementDao().get(key)
        if (existing?.unlockedAt != null) return false
        db.achievementDao().upsert(AchievementEntity(key, System.currentTimeMillis()))
        return true
    }

    /**
     * Devuelve la unión del catálogo fijo + el catálogo combinatorio random,
     * con el `unlockedAt` poblado donde corresponda.
     */
    suspend fun all(): List<AchievementWithMeta> {
        val unlocked = db.achievementDao().getAll().associateBy { it.key }
        val classic = AchievementsCatalog.all.map { meta ->
            AchievementWithMeta(
                key = meta.key,
                title = meta.title,
                description = meta.description,
                emoji = meta.emoji,
                unlockedAt = unlocked[meta.key]?.unlockedAt,
                category = "Clásicos",
            )
        }
        val random = RandomTrophyCatalog.all().map { meta ->
            AchievementWithMeta(
                key = meta.key,
                title = meta.title,
                description = meta.description,
                emoji = meta.emoji,
                unlockedAt = unlocked[meta.key]?.unlockedAt,
                category = "Aleatorios",
            )
        }
        return classic + random
    }

    /** Sólo los desbloqueados (para vitrina / Home). */
    suspend fun unlockedOnly(): List<AchievementWithMeta> = all().filter { it.unlockedAt != null }

    /**
     * Reevalúa logros derivables del estado actual (racha, % meta, gasto vs mes pasado).
     * Devuelve los nuevos desbloqueos para mostrar notif.
     */
    suspend fun evaluate(home: HomeUiModel, repository: ExpenseRepository): List<AchievementMeta> {
        val unlocks = mutableListOf<AchievementMeta>()
        val streak = home.streakDaysNoSpend
        if (streak >= 3) maybeUnlock(unlocks, AchievementsCatalog.STREAK_3)
        if (streak >= 7) maybeUnlock(unlocks, AchievementsCatalog.STREAK_7)
        if (streak >= 14) maybeUnlock(unlocks, AchievementsCatalog.STREAK_14)
        if (streak >= 30) maybeUnlock(unlocks, AchievementsCatalog.STREAK_30)

        val pct = home.globalProgress?.toDouble() ?: 1.1
        if (home.globalGoalPesos != null && pct < 1.0 && home.expenseCount > 0) {
            val today = LocalDate.now()
            val ym = home.yearMonth
            val isPastMonth = ym.year < today.year || (ym.year == today.year && ym.monthValue < today.monthValue)
            if (isPastMonth) maybeUnlock(unlocks, AchievementsCatalog.MONTH_GREEN)
        }

        if (home.expenseCount >= 1) maybeUnlock(unlocks, AchievementsCatalog.FIRST_EXPENSE)
        if (home.expenseCount >= 50) maybeUnlock(unlocks, AchievementsCatalog.FIFTY_EXPENSES)

        // === Catálogo combinatorio (random trophies) ===
        val today = LocalDate.now()
        val lifetime = repository.lifetimeStats()
        val ctx = RandomTrophyCatalog.EvalContext(
            home = home,
            today = today,
            countByCategorySlug = lifetime.countByCategorySlug,
            sumByCategorySlug = lifetime.sumByCategorySlug,
            lastExpenseHour = lifetime.lastExpenseHour,
            lifetimeExpenseCount = lifetime.totalCount,
            lifetimeSpendPesos = lifetime.totalPesos,
            holiday = HolidayCalendar.forDate(today),
        )
        val unlockedKeys = RandomTrophyCatalog.evaluate(ctx)
        if (unlockedKeys.isNotEmpty()) {
            val randomMap = RandomTrophyCatalog.all().associateBy { it.key }
            for (key in unlockedKeys) {
                if (unlock(key)) {
                    randomMap[key]?.let { unlocks += it }
                }
            }
        }

        return unlocks
    }

    private suspend fun maybeUnlock(into: MutableList<AchievementMeta>, key: String) {
        if (unlock(key)) {
            AchievementsCatalog.all.firstOrNull { it.key == key }?.let { into += it }
        }
    }

    /** Sin estado: setea las filas iniciales para que aparezcan bloqueadas en la UI. */
    suspend fun refreshAll(repository: ExpenseRepository) {
        val current = db.achievementDao().getAll().map { it.key }.toSet()
        AchievementsCatalog.all.forEach { meta ->
            if (meta.key !in current) {
                db.achievementDao().upsert(AchievementEntity(meta.key, null))
            }
        }
        // OJO: NO seedeamos los 1000+ random porque inflan la base.
        // Se "crean" cuando se desbloquean. La pantalla muestra el catálogo lazy.
    }

    /**
     * Trofeo del día (random determinístico). Si el usuario lo cumple
     * hoy, lo guardamos como key especial para que no se borre.
     */
    fun trophyOfDay(today: LocalDate = LocalDate.now()): AchievementMeta =
        RandomTrophyCatalog.trophyOfDay(today)
}
