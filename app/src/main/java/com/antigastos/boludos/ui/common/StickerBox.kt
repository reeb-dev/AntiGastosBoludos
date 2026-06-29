package com.antigastos.boludos.ui.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.antigastos.boludos.domain.CopyMood

/**
 * Wrapper que muestra un emoticón grande estilo "sticker" con fondo
 * gradiente y micro-pulso. `key` debe ser estable mientras el caller no
 * haga shuffle, así no cambia de emoji en cada recomposición.
 *
 * Mantenemos esta firma porque ya se invoca desde `PersonalityGif`.
 */
@Composable
fun StickerBox(
    mood: CopyMood,
    headline: String? = null,
    subline: String? = null,
    key: Long = 0L,
    modifier: Modifier = Modifier,
) {
    ArgSticker(
        mood = mood,
        key = key,
        subline = subline ?: headline,
        modifier = modifier.fillMaxSize(),
    )
}
