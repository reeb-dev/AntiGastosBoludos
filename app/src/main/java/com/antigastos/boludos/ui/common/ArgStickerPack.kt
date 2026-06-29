package com.antigastos.boludos.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigastos.boludos.domain.CopyMood
import kotlin.math.abs

/**
 * Catálogo de "stickers argentinos" — implementado con emoticones grandes
 * sobre un fondo con gradiente. Antes había vectoriales en Canvas, ahora
 * preferimos emojis: pesan menos, escalan automáticamente con el sistema
 * y se ven más cercanos al tono de WhatsApp.
 *
 * Mantenemos la API ([ArgSticker], [ArgStickerKind]) para no romper a los
 * callers. Se elige uno mezclando un seed con el [CopyMood] así no queda
 * "atrapado" en el subconjunto del mood.
 */
enum class ArgStickerKind {
    MATE,
    BANDERA,
    PELOTA,
    ASADO,
    ALFAJOR,
    EMPANADA,
    BILLETERA,
    BONDI,
    TERMO,
    OBELISCO,
    CHORI,
    LLAMA,
    BANDERA_CRY,
    CALAVERA,
    ORO_DROP,
}

private data class StickerDef(
    val kind: ArgStickerKind,
    val emoji: String,
    val mood: CopyMood,
    val bg: Color,
    val accent: Color,
    val tagline: String,
)

private val pack: List<StickerDef> = listOf(
    StickerDef(ArgStickerKind.MATE, "🧉", CopyMood.NEUTRAL, Color(0xFFE7F1FB), Color(0xFF1F4E8A), "Bancáte el mate"),
    StickerDef(ArgStickerKind.BANDERA, "🇦🇷", CopyMood.CHEER, Color(0xFFE6F2FB), Color(0xFF1F4E8A), "Vamos Argentina"),
    StickerDef(ArgStickerKind.PELOTA, "⚽", CopyMood.CHEER, Color(0xFFE6F4EA), Color(0xFF2E7D32), "GOOOL del ahorro"),
    StickerDef(ArgStickerKind.ASADO, "🥩", CopyMood.BURN, Color(0xFFFFF4E0), Color(0xFFB97A0A), "Se quemó el asado"),
    StickerDef(ArgStickerKind.ALFAJOR, "🍫", CopyMood.NEUTRAL, Color(0xFFFCEFE2), Color(0xFF7B4A1A), "Te durá poco"),
    StickerDef(ArgStickerKind.EMPANADA, "🥟", CopyMood.NEUTRAL, Color(0xFFFFF4E0), Color(0xFF8E5B12), "Cada empanada cuenta"),
    StickerDef(ArgStickerKind.BILLETERA, "💸", CopyMood.ALARM, Color(0xFFFDE7EA), Color(0xFFD7263D), "Vuela la guita"),
    StickerDef(ArgStickerKind.BONDI, "🚌", CopyMood.BURN, Color(0xFFEAF1F8), Color(0xFF2A4D7A), "Subite a la SUBE"),
    StickerDef(ArgStickerKind.TERMO, "🫖", CopyMood.NEUTRAL, Color(0xFFE9F5EE), Color(0xFF2E7D32), "Termo escuchando"),
    StickerDef(ArgStickerKind.OBELISCO, "🗼", CopyMood.CHEER, Color(0xFFE7F1FB), Color(0xFF1F4E8A), "Cabeza fría"),
    StickerDef(ArgStickerKind.CHORI, "🌭", CopyMood.BURN, Color(0xFFFFF4E0), Color(0xFFB97A0A), "Modo chori"),
    StickerDef(ArgStickerKind.LLAMA, "🔥", CopyMood.ALARM, Color(0xFFFFEAE0), Color(0xFFE0480A), "Se prendió fuego"),
    StickerDef(ArgStickerKind.BANDERA_CRY, "😭", CopyMood.ALARM, Color(0xFFEAF1F8), Color(0xFF1F4E8A), "Lloró la patria"),
    StickerDef(ArgStickerKind.CALAVERA, "💀", CopyMood.ALARM, Color(0xFFF1E6E9), Color(0xFF6F1F2B), "Game over financiero"),
    StickerDef(ArgStickerKind.ORO_DROP, "🏆", CopyMood.CHEER, Color(0xFFFFF8DC), Color(0xFFC6A000), "DROP LEGENDARIO"),
)

/**
 * Mezcla el seed con el mood y devuelve un sticker. No filtramos por mood:
 * preferimos variedad y que el catálogo entero esté siempre disponible.
 */
private fun pickByKey(mood: CopyMood, key: Long): StickerDef {
    val mixed = key xor (mood.ordinal.toLong() shl 20) xor (key ushr 17)
    val idx = abs(mixed.toInt() xor (mixed shr 32).toInt()) % pack.size
    return pack[idx]
}

/**
 * Tarjeta de "sticker" con un emoticón grande, un fondo en gradiente y un
 * micro-pulso para que no quede estático.
 *
 * @param key seed estable (templateId.hash + tick) para mantener el mismo
 *   sticker mientras no haya shuffle.
 * @param forceLegendary fuerza el emoji 🏆 con el fondo dorado (drop ~1%).
 */
@Composable
fun ArgSticker(
    mood: CopyMood,
    key: Long,
    subline: String? = null,
    forceLegendary: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val def = if (forceLegendary) {
        pack.first { it.kind == ArgStickerKind.ORO_DROP }
    } else {
        pickByKey(mood, key)
    }

    val infinite = rememberInfiniteTransition(label = "stickerPulse")
    val scale by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "stickerPulseV",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(def.bg, def.bg.copy(alpha = 0.55f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    def.emoji,
                    fontSize = 96.sp,
                    modifier = Modifier.scale(scale),
                )
            }
            Text(
                def.tagline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = def.accent,
                textAlign = TextAlign.Center,
            )
            if (!subline.isNullOrBlank()) {
                Text(
                    subline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
