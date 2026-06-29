package com.antigastos.boludos.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Banner adaptativo anclado (anchored) al ancho de pantalla.
 * Pausa/destruye el [AdView] al salir de la composición.
 */
@Composable
fun BannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobIds.bannerListPie,
) {
    val context = LocalContext.current
    val adView = remember(adUnitId) {
        val widthPx = context.resources.displayMetrics.widthPixels
        val widthDp = (widthPx / context.resources.displayMetrics.density).toInt()
        AdView(context).apply {
            setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp),
            )
            this.adUnitId = adUnitId
            adListener = object : AdListener() {
                override fun onAdImpression() {
                    AdMonetizationTracker.onBannerImpression()
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose {
            adView.destroy()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { adView },
    )
}
