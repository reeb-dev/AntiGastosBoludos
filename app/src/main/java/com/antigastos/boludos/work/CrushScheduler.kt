package com.antigastos.boludos.work

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object CrushScheduler {

    private fun uniqueName(crushId: Long) = "antigastos_crush_$crushId"

    fun schedule(context: Context, crushId: Long) {
        val input = Data.Builder()
            .putLong(CrushCooldownWorker.KEY_CRUSH_ID, crushId)
            .build()
        val req = OneTimeWorkRequestBuilder<CrushCooldownWorker>()
            .setInitialDelay(24L, TimeUnit.HOURS)
            .setInputData(input)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName(crushId),
            ExistingWorkPolicy.REPLACE,
            req,
        )
    }

    fun cancel(context: Context, crushId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName(crushId))
    }
}
