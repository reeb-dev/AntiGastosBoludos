package com.antigastos.boludos.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.notifications.Notifier

/**
 * Pasadas 24 h desde que el usuario mandó un gasto a la “heladera”, avisamos
 * para que confirme o descarte.
 */
class CrushCooldownWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AntiGastosApplication ?: return Result.success()
        val id = inputData.getLong(KEY_CRUSH_ID, -1L)
        if (id <= 0L) return Result.success()
        val settings = app.settingsRepository.getSnapshot()
        if (!settings.crushCooldownEnabled) return Result.success()

        val crush = app.repository.getPendingCrush(id) ?: return Result.success()
        val cat = app.database.categoryDao().getById(crush.categoryId)
        val catName = cat?.name ?: "categoría"
        val amount = MoneyFormat.formatPesos(crush.amountPesos)
        val noteBit = crush.note?.takeIf { it.isNotBlank() }?.let { " · «${it.take(40)}»" } ?: ""
        Notifier.postCrushReminder(
            context = app,
            crushId = id,
            title = "🧊 ¿Seguís con el antojo?",
            body = "Son $amount en $catName$noteBit. ¿Lo cargamos o lo dejamos morir?",
        )
        return Result.success()
    }

    companion object {
        const val KEY_CRUSH_ID = "crush_id"
    }
}
