package com.antigastos.boludos.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.antigastos.boludos.AntiGastosApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Recibe taps de "+5 mil" / "+10 mil" desde la notificación fija de racha.
 */
class QuickExpenseReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val pesos = intent.getLongExtra(EXTRA_PESOS, 0L)
        if (pesos <= 0L) return
        val app = context.applicationContext as? AntiGastosApplication ?: return
        val pending = goAsync()
        app.applicationScope.launch(Dispatchers.IO) {
            try {
                val cat = app.repository.quickExpenseDefaultCategoryId() ?: return@launch
                app.repository.insertExpense(
                    pesos,
                    cat,
                    "Atajo +${pesos / 1000L}k (notificación)",
                    System.currentTimeMillis(),
                )
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION = "com.antigastos.boludos.ACTION_QUICK_EXPENSE"
        const val EXTRA_PESOS = "pesos"

        fun pendingIntent(ctx: Context, pesos: Long, requestCode: Int): PendingIntent {
            val i = Intent(ctx, QuickExpenseReceiver::class.java).apply {
                action = ACTION
                putExtra(EXTRA_PESOS, pesos)
            }
            return PendingIntent.getBroadcast(
                ctx,
                requestCode,
                i,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
