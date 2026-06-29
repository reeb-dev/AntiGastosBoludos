package com.antigastos.boludos.ads

import android.graphics.Typeface
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Card nativa de AdMob insertada entre ítems de la lista de gastos.
 * Usa el formato native advanced (test ID) y renderiza headline + body +
 * icono dentro de un NativeAdView manual (sin XML layout).
 */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        val preloaded = AdHelper.consumePreloadedNative()
        if (preloaded != null) {
            nativeAd = preloaded
        } else {
            val loader = AdLoader.Builder(context, AdMobIds.nativeListAfter5)
                .forNativeAd { ad ->
                    nativeAd?.destroy()
                    nativeAd = ad
                }
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .build(),
                )
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                        // Sin UI de error: la lista sigue sin el bloque nativo.
                    }
                })
                .build()
            loader.loadAd(AdRequest.Builder().build())
        }

        onDispose {
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    val ad = nativeAd ?: return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            factory = { ctx ->
                NativeAdView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                    )
                    val container = LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(16, 12, 16, 12)
                    }

                    val adLabel = TextView(ctx).apply {
                        text = "Publicidad"
                        textSize = 10f
                        setTypeface(null, Typeface.BOLD)
                        alpha = 0.5f
                    }
                    container.addView(adLabel)

                    val headlineView = TextView(ctx).apply {
                        textSize = 15f
                        setTypeface(null, Typeface.BOLD)
                        setPadding(0, 8, 0, 4)
                    }
                    container.addView(headlineView)
                    this.headlineView = headlineView

                    val bodyView = TextView(ctx).apply {
                        textSize = 13f
                        maxLines = 3
                    }
                    container.addView(bodyView)
                    this.bodyView = bodyView

                    val iconView = ImageView(ctx).apply {
                        val sz = (40 * ctx.resources.displayMetrics.density).toInt()
                        layoutParams = LinearLayout.LayoutParams(sz, sz).apply {
                            topMargin = 8
                        }
                    }
                    container.addView(iconView)
                    this.iconView = iconView

                    addView(container)
                }
            },
            update = { adView ->
                (adView.headlineView as? TextView)?.text = ad.headline
                (adView.bodyView as? TextView)?.text = ad.body
                val icon = ad.icon
                if (icon != null) {
                    (adView.iconView as? ImageView)?.setImageDrawable(icon.drawable)
                    adView.iconView?.visibility = android.view.View.VISIBLE
                } else {
                    adView.iconView?.visibility = android.view.View.GONE
                }
                adView.setNativeAd(ad)
            },
        )
    }
}
