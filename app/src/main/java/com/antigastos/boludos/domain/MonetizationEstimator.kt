package com.antigastos.boludos.domain

import com.antigastos.boludos.data.local.entity.SettingsEntity
import kotlin.math.roundToLong

data class AdEcpmAssumptions(
    val bannerPer1000: Long = 180L,
    val nativePer1000: Long = 320L,
    val rewardedPerCompletion: Long = 45L,
)

data class MonetizationSnapshot(
    val bannerImpressions: Long,
    val nativeImpressions: Long,
    val rewardedCompleted: Long,
    val estimatedAdsPesos: Long,
    val estimatedTotalPesos: Long,
    val activeAdUsersEstimate: String,
)

object MonetizationEstimator {

    fun snapshot(settings: SettingsEntity, ecpm: AdEcpmAssumptions = AdEcpmAssumptions()): MonetizationSnapshot {
        val ads = estimateAdsPesos(
            bannerImpressions = settings.adStatBannerImpressions,
            nativeImpressions = settings.adStatNativeImpressions,
            rewardedCompleted = settings.adStatRewardedCompleted,
            ecpm = ecpm,
        )
        return MonetizationSnapshot(
            bannerImpressions = settings.adStatBannerImpressions,
            nativeImpressions = settings.adStatNativeImpressions,
            rewardedCompleted = settings.adStatRewardedCompleted,
            estimatedAdsPesos = ads,
            estimatedTotalPesos = ads,
            activeAdUsersEstimate = "Este dispositivo muestra publicidad",
        )
    }

    fun estimateAdsPesos(
        bannerImpressions: Long,
        nativeImpressions: Long,
        rewardedCompleted: Long,
        ecpm: AdEcpmAssumptions = AdEcpmAssumptions(),
    ): Long {
        val banner = (bannerImpressions * ecpm.bannerPer1000) / 1000.0
        val native = (nativeImpressions * ecpm.nativePer1000) / 1000.0
        val rewarded = rewardedCompleted * ecpm.rewardedPerCompletion.toDouble()
        return (banner + native + rewarded).roundToLong()
    }
}
