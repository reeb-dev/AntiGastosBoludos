package com.antigastos.boludos.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.antigastos.boludos.AntiGastosApplication

class SubsSeederWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AntiGastosApplication ?: return Result.success()
        // Idempotente por día. El seed real del repo también lo es por
        // `lastSeededPeriod`, esto es solo un cinturón extra contra reintentos.
        if (!WorkerNotificationState.tryConsumeSubsSeed(applicationContext)) {
            return Result.success()
        }
        runCatching { app.repository.seedSubscriptionsForCurrentMonth() }
        return Result.success()
    }
}
