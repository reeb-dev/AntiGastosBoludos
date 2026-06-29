package com.antigastos.boludos.ui.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.domain.CopyMood
import com.antigastos.boludos.domain.GifKeyword
import com.antigastos.boludos.ui.common.StickerBox

private const val TAG = "PersonalityGif"

private sealed interface GifStatus {
    data object Sticker : GifStatus
    data object Loading : GifStatus
    data class Loaded(val url: String, val source: String) : GifStatus
}

@Composable
fun PersonalityGif(
    templateId: String,
    mood: CopyMood,
    slug: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as AntiGastosApplication
    val repo = app.gifRepository
    val memeApi = app.memeApi
    val settings by app.settingsRepository.flow.collectAsState(initial = SettingsEntity())

    val anyKey = remember(repo) { repo.anyKeyConfigured() }
    val onlyLocal = settings.localStickersOnly

    var shuffleTick by remember(templateId) { mutableStateOf(0) }
    val seed = remember(templateId, slug, shuffleTick) {
        (templateId.hashCode().toLong() * 31 +
            (slug?.hashCode() ?: 0)) * 1009L + shuffleTick.toLong()
    }
    val keyword = remember(templateId, mood, slug, seed) {
        GifKeyword.queryFor(templateId, mood, slug, seed)
    }

    var status: GifStatus by remember(templateId) {
        mutableStateOf(if (onlyLocal) GifStatus.Sticker else GifStatus.Loading)
    }

    LaunchedEffect(keyword, shuffleTick, onlyLocal) {
        if (onlyLocal) {
            status = GifStatus.Sticker
            return@LaunchedEffect
        }
        status = GifStatus.Loading

        // 1) GIF (Giphy/Tenor/Klipy) si hay alguna API key
        if (anyKey) {
            Log.d(TAG, "Buscando GIF: '$keyword'")
            val gifUrl = runCatching { repo?.randomGifUrl(keyword) }
                .onFailure { Log.e(TAG, "Error consultando proveedor de GIFs", it) }
                .getOrNull()
            if (!gifUrl.isNullOrBlank()) {
                status = GifStatus.Loaded(gifUrl, "GIF")
                return@LaunchedEffect
            }
            Log.w(TAG, "Sin GIF para '$keyword'. Probando meme argentino…")
        }

        // 2) Fallback opcional: meme-api.com
        val meme = runCatching { memeApi?.randomArgMeme() }
            .onFailure { Log.e(TAG, "Error consultando meme-api", it) }
            .getOrNull()
        if (meme != null) {
            status = GifStatus.Loaded(meme.url, "r/${meme.subreddit}")
            return@LaunchedEffect
        }

        // 3) Emoticón local Compose (siempre disponible)
        Log.w(TAG, "Sin GIF ni meme. Emoticón local.")
        status = GifStatus.Sticker
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable { shuffleTick += 1 },
        contentAlignment = Alignment.Center,
    ) {
        when (val s = status) {
            GifStatus.Sticker -> StickerBox(
                mood = mood,
                key = seed,
                modifier = Modifier.fillMaxSize(),
            )
            GifStatus.Loading -> StickerBox(
                mood = mood,
                key = seed,
                subline = "Cargando…",
                modifier = Modifier.fillMaxSize(),
            )
            is GifStatus.Loaded -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(s.url)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Reacción animada",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x66000000))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .alpha(0.85f),
                ) {
                    Text(
                        s.source,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0x66000000))
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .alpha(0.85f),
        ) {
            Text(
                "🎲 Otro",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
