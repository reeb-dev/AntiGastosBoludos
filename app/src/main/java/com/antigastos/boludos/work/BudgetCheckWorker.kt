package com.antigastos.boludos.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.notifications.Notifier
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth

/**
 * Cada N horas evalúa:
 * 1) Si el gasto del mes >= 80% de la meta global → notif "te queda poco margen"
 * 2) Si quedan <= 3 días y vas a romper la meta → "alerta fin de mes"
 * 3) Logros derivados (rachas, primer gasto, etc.)
 * 4) Pactos vencidos sin romper → marcarlos como completos + logro
 */
class BudgetCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AntiGastosApplication ?: return Result.success()
        val settings = app.settingsRepository.getSnapshot()

        // Si WorkManager nos despertó de más en el mismo día (reintentos,
        // cambio de zona horaria, etc.) salimos sin notificar.
        if (!WorkerNotificationState.tryConsumeBudgetCheck(applicationContext)) {
            return Result.success()
        }

        val ym = YearMonth.now()
        val home = runCatching { app.repository.observeHome(ym).first() }.getOrNull()
            ?: return Result.success()

        if (settings.budgetAlertsEnabled) {
            val pct = home.globalProgress?.toDouble() ?: 0.0
            if (pct >= 0.8 && pct < 1.0) {
                Notifier.postReminder(
                    applicationContext,
                    "⚠️ Cuidado con la guita",
                    "Ya gastaste el ${(pct * 100).toInt()}% de la meta del mes. Bajá un cambio antes de fundirte.",
                )
            }
            val today = LocalDate.now()
            val daysLeft = ym.lengthOfMonth() - today.dayOfMonth + 1
            if (daysLeft in 1..3 && pct >= 0.85) {
                Notifier.postFail(
                    applicationContext,
                    "🚨 Fin de mes apretado",
                    "Quedan $daysLeft días y ya consumiste ${(pct * 100).toInt()}% de la meta. Estás al borde.",
                )
            }
            if (pct >= 1.0) {
                Notifier.postFail(
                    applicationContext,
                    "💣 Te pasaste la meta",
                    "Excediste la meta del mes (${(pct * 100).toInt()}%). Cuidá lo que te queda hasta fin de mes.",
                )
            }
        }

        val unlocks = app.achievementsRepository.evaluate(home, app.repository)
        unlocks.forEach { meta ->
            Notifier.postReward(
                applicationContext,
                "${meta.emoji} Nuevo logro",
                "Desbloqueaste: ${meta.title}",
            )
            app.scoreRepository.applyDelta(
                com.antigastos.boludos.domain.ScoreEngine.evaluateAchievementUnlock(meta.key, meta.title),
            )
        }

        // Evaluación diaria del puntaje (idempotente por firma del día).
        val deltas = app.scoreRepository.evaluateDay(home)
        deltas.forEach { d ->
            when (d.kind) {
                com.antigastos.boludos.domain.ScoreEngine.Kind.REWARD ->
                    Notifier.postReward(
                        applicationContext,
                        "${d.emoji} ${d.label}",
                        "Sumaste ${d.points} puntos en tu ranking. Seguí así.",
                    )
                com.antigastos.boludos.domain.ScoreEngine.Kind.FAIL ->
                    Notifier.postFail(
                        applicationContext,
                        "${d.emoji} ${d.label}",
                        "Te restaron ${kotlin.math.abs(d.points)} puntos del ranking. Cuidá los gastos.",
                    )
            }
        }

        // Pactos vencidos sin romperse → completos.
        val completed = app.pactRepository.completeExpired()
        if (completed.isNotEmpty()) {
            app.achievementsRepository.unlock(com.antigastos.boludos.domain.AchievementsCatalog.PACT_DONE)
            Notifier.postReward(
                applicationContext,
                "🤝 Pacto cumplido",
                "Bancaste el pacto. Te ganaste un capricho y un trofeo.",
            )
        }

        return Result.success()
    }
}
