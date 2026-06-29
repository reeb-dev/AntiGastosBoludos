package com.antigastos.boludos.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Helper centralizado para cargar y mostrar ads.
 * IDs en [AdMobIds] (debug = test de Google, release = producción).
 * El consentimiento UMP se pide desde [AdConsent] antes de inicializar ads.
 */
object AdHelper {

    private const val TAG = "AdHelper"

    /** Mensaje estándar cuando el rewarded aún no terminó de cargar. */
    const val REWARDED_NOT_READY_MESSAGE =
        "El video no está listo, intentá en unos segundos"

    private fun rewardedAdUnit(slot: RewardedSlot): String = when (slot) {
        RewardedSlot.STREAK_SHIELD -> AdMobIds.rewardedStreakShield
        RewardedSlot.DOUBLE_LOTERIA -> AdMobIds.rewardedLoteriaDouble
        RewardedSlot.EXTRA_RULETA -> AdMobIds.rewardedRuletaExtra
    }

    /** Slots de rewarded ads que la app puede precargar en paralelo. */
    enum class RewardedSlot { STREAK_SHIELD, DOUBLE_LOTERIA, EXTRA_RULETA }

    private val rewardedAds = mutableMapOf<RewardedSlot, RewardedAd?>()
    private val loadingSlots = mutableSetOf<RewardedSlot>()

    private var preloadedNative: NativeAd? = null
    private var nativeLoading = false

    /** Precarga un slot específico de rewarded. Si ya está cargado, no hace nada. */
    fun preloadRewarded(context: Context, slot: RewardedSlot = RewardedSlot.STREAK_SHIELD) {
        if (rewardedAds[slot] != null || slot in loadingSlots) return
        loadingSlots += slot
        val request = AdRequest.Builder().build()
        RewardedAd.load(context, rewardedAdUnit(slot), request, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAds[slot] = ad
                loadingSlots -= slot
                Log.d(TAG, "Rewarded precargado [${slot.name}].")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                rewardedAds[slot] = null
                loadingSlots -= slot
                Log.w(TAG, "Error cargando rewarded [${slot.name}]: ${error.message}")
            }
        })
    }

    /** Precarga todos los slots de rewarded de una vez. */
    fun preloadAllRewarded(context: Context) {
        RewardedSlot.entries.forEach { preloadRewarded(context, it) }
    }

    /** True si hay un rewarded listo para mostrar en el slot indicado. */
    fun isRewardedReady(slot: RewardedSlot = RewardedSlot.STREAK_SHIELD): Boolean =
        rewardedAds[slot] != null

    /**
     * Precarga un native ad para la lista de gastos (cuando hay más de 5 ítems).
     * [NativeAdCard] puede consumirlo con [consumePreloadedNative].
     */
    fun preloadNative(context: Context) {
        if (preloadedNative != null || nativeLoading) return
        nativeLoading = true
        val appContext = context.applicationContext
        AdLoader.Builder(appContext, AdMobIds.nativeListAfter5)
            .forNativeAd { ad ->
                preloadedNative?.destroy()
                preloadedNative = ad
                nativeLoading = false
                AdMonetizationTracker.onNativeImpression()
                Log.d(TAG, "Native precargado.")
            }
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build(),
            )
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    nativeLoading = false
                    Log.w(TAG, "Error precargando native: ${error.message}")
                }
            })
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    /** Toma el native precargado (si hay) y limpia el slot de precarga. */
    fun consumePreloadedNative(): NativeAd? {
        val ad = preloadedNative
        preloadedNative = null
        return ad
    }

    /**
     * Muestra el rewarded ad del [slot]. Invoca [onRewardEarned] si el usuario
     * completa el video. Recarga automáticamente después de mostrarlo.
     *
     * @return `true` si se mostró el anuncio; `false` si no estaba listo (ya dispara precarga).
     */
    fun showRewarded(
        activity: Activity,
        slot: RewardedSlot = RewardedSlot.STREAK_SHIELD,
        onRewardEarned: () -> Unit,
    ): Boolean {
        val ad = rewardedAds[slot]
        if (ad == null) {
            Log.w(TAG, "Rewarded [${slot.name}] no disponible. Re-cargando...")
            preloadRewarded(activity, slot)
            return false
        }
        AdMonetizationTracker.onRewardedOffered()
        ad.show(activity) { _ ->
            AdMonetizationTracker.onRewardedCompleted()
            onRewardEarned()
        }
        rewardedAds[slot] = null
        preloadRewarded(activity, slot)
        return true
    }
}
