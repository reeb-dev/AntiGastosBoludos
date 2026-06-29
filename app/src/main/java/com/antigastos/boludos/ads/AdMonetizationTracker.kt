package com.antigastos.boludos.ads

import com.antigastos.boludos.AntiGastosApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Contadores locales para estimar ingresos por publicidad (panel dev). */
object AdMonetizationTracker {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var app: AntiGastosApplication? = null

    fun attach(application: AntiGastosApplication) {
        app = application
    }

    fun onBannerImpression() = bump { it.copy(adStatBannerImpressions = it.adStatBannerImpressions + 1) }

    fun onNativeImpression() = bump { it.copy(adStatNativeImpressions = it.adStatNativeImpressions + 1) }

    fun onRewardedOffered() = bump { it.copy(adStatRewardedOffers = it.adStatRewardedOffers + 1) }

    fun onRewardedCompleted() = bump { it.copy(adStatRewardedCompleted = it.adStatRewardedCompleted + 1) }

    private fun bump(transform: (com.antigastos.boludos.data.local.entity.SettingsEntity) ->
        com.antigastos.boludos.data.local.entity.SettingsEntity,
    ) {
        val a = app ?: return
        scope.launch {
            a.settingsRepository.update(transform)
        }
    }
}
