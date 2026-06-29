package com.antigastos.boludos.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.antigastos.boludos.domain.Consejo
import com.antigastos.boludos.domain.ConsejoKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Modal a pantalla completa con fondo animado según el tipo de consejo:
 * premio → confeti / dorado; sugerencia → ondas suaves; freno → alerta ámbar;
 * emergencia → pulso rojo y “scan”.
 */
@Composable
fun ConsejoEventModal(
    consejo: Consejo?,
    onDismiss: () -> Unit,
    onCyclePersona: (() -> Unit)? = null,
) {
    val c = consejo ?: return

    // El timer se reinicia cuando cambia el id O la persona, para que la
    // ruleta no cierre el modal de golpe.
    val timerKey = "${c.id}|${c.personaKey ?: "none"}"
    var progress by remember(timerKey) { mutableStateOf(1f) }

    LaunchedEffect(timerKey) {
        val total = c.durationMs
        val tickMs = 50L
        var elapsed = 0L
        while (elapsed < total) {
            kotlinx.coroutines.delay(tickMs)
            elapsed += tickMs
            progress = ((total - elapsed).toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
            ) {}
            ConsejoAmbientBackground(
                kind = c.kind,
                seed = c.id.hashCode(),
                onDismissScrim = onDismiss,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* la tarjeta no cierra al tocar el texto */ },
                        ),
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                    color = cardSurfaceColor(c.kind),
                ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardAccent(c.kind)),
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White.copy(alpha = 0.95f),
                            trackColor = cardAccent(c.kind),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ConsejoHeroIcon(kind = c.kind)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                c.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = cardAccent(c.kind),
                            )
                            // Chip + ruleta de personalidad: solo si hay voz seteada.
                            if (c.personaEmoji != null && c.personaName != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    AssistChip(
                                        onClick = { onCyclePersona?.invoke() },
                                        enabled = onCyclePersona != null,
                                        leadingIcon = {
                                            Text(
                                                c.personaEmoji,
                                                style = MaterialTheme.typography.bodyLarge,
                                            )
                                        },
                                        label = {
                                            Text(
                                                "voz: ${c.personaName}",
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = cardAccent(c.kind).copy(alpha = 0.10f),
                                            labelColor = cardAccent(c.kind),
                                            leadingIconContentColor = cardAccent(c.kind),
                                        ),
                                    )
                                    if (onCyclePersona != null) {
                                        IconButton(
                                            onClick = onCyclePersona,
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                Icons.Filled.Casino,
                                                contentDescription = "Cambiar personalidad",
                                                tint = cardAccent(c.kind),
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                c.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Start,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (onCyclePersona != null && c.personaKey != null)
                                    "🎰 tocá la ruleta para cambiar la voz · Tocá fuera para cerrar"
                                else "Tocá fuera para cerrar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Cerrar",
                                tint = cardAccent(c.kind),
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun ConsejoHeroIcon(kind: ConsejoKind) {
    val scale by rememberInfiniteTransition(label = "heroPulse").animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heroPulseV",
    )
    when (kind) {
        ConsejoKind.PREMIO -> Text(
            "🏆",
            modifier = Modifier
                .scale(scale)
                .size(52.dp),
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
        ConsejoKind.SUGERENCIA -> Text(
            "💡",
            modifier = Modifier
                .scale(scale * 0.98f)
                .size(48.dp),
            style = MaterialTheme.typography.displaySmall,
        )
        ConsejoKind.FRENO -> Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .scale(scale),
            tint = Color(0xFFE65100),
        )
        ConsejoKind.EMERGENCIA -> Text(
            "💀",
            modifier = Modifier
                .scale(scale)
                .size(52.dp),
            style = MaterialTheme.typography.displaySmall,
        )
    }
}

private fun cardSurfaceColor(kind: ConsejoKind): Color = when (kind) {
    ConsejoKind.PREMIO -> Color(0xFFF5FFF7)
    ConsejoKind.SUGERENCIA -> Color(0xFFF5F9FF)
    ConsejoKind.FRENO -> Color(0xFFFFFBF0)
    ConsejoKind.EMERGENCIA -> Color(0xFFFFF5F5)
}

private fun cardAccent(kind: ConsejoKind): Color = when (kind) {
    ConsejoKind.PREMIO -> Color(0xFF2E7D32)
    ConsejoKind.SUGERENCIA -> Color(0xFF1F4E8A)
    ConsejoKind.FRENO -> Color(0xFFE65100)
    ConsejoKind.EMERGENCIA -> Color(0xFFC62828)
}

@Composable
private fun ConsejoAmbientBackground(
    kind: ConsejoKind,
    seed: Int,
    onDismissScrim: () -> Unit,
) {
    val infinite = rememberInfiniteTransition(label = "ambient")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    val rng = remember(seed) { Random(seed) }
    val particles = remember(seed, kind) {
        List(48) {
            Particle(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                r = 3f + rng.nextFloat() * 5f,
                speed = 0.15f + rng.nextFloat() * 0.35f,
                hue = rng.nextInt(5),
            )
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.85f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismissScrim,
            ),
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        when (kind) {
            ConsejoKind.PREMIO -> {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF81C784).copy(alpha = 0.35f * pulse),
                            Color(0xFFFFF176).copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                        center = Offset(cx, cy * 0.85f),
                        radius = w * 0.9f,
                    ),
                )
                particles.forEachIndexed { i, p ->
                    val drift = (phase + i * 0.07f) % 1f
                    val y = (p.y + drift * p.speed) % 1f
                    val wobble = sin((drift + i) * 2.0 * PI).toFloat() * 0.03f
                    val x = (p.x + wobble).coerceIn(0f, 1f)
                    val colors = listOf(
                        Color(0xFFFFD54F),
                        Color(0xFF66BB6A),
                        Color(0xFFFFFFFF),
                        Color(0xFFFFB74D),
                        Color(0xFFA5D6A7),
                    )
                    drawCircle(
                        color = colors[p.hue % colors.size].copy(alpha = 0.55f),
                        radius = p.r * 1.8f,
                        center = Offset(x * w, y * h),
                    )
                }
            }
            ConsejoKind.SUGERENCIA -> {
                val ox = sin(phase * 2.0 * PI).toFloat() * w * 0.08f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF64B5F6).copy(alpha = 0.22f),
                            Color(0xFF1565C0).copy(alpha = 0.08f),
                            Color(0xFF4FC3F7).copy(alpha = 0.18f),
                        ),
                        start = Offset(ox, 0f),
                        end = Offset(w + ox, h),
                    ),
                )
                repeat(12) { i ->
                    val a = (phase * 360.0 + i * 30.0) * PI / 180.0
                    val r = w * (0.25f + (i % 4) * 0.06f)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = 40f + i * 6f,
                        center = Offset(
                            cx + cos(a).toFloat() * r * 0.3f,
                            cy + sin(a).toFloat() * r * 0.25f,
                        ),
                    )
                }
            }
            ConsejoKind.FRENO -> {
                val stripeW = 56f
                val offset = phase * stripeW * 2f
                withTransform({
                    rotate(degrees = -35f, pivot = Offset(cx, cy))
                }) {
                    val startX = -w + offset % (stripeW * 2f)
                    val count = ((w * 3f) / stripeW).toInt() + 4
                    for (k in 0..count) {
                        val x = startX + k * stripeW
                        drawRect(
                            color = if ((k % 2) == 0) {
                                Color(0xFFFFC107).copy(alpha = 0.25f)
                            } else {
                                Color(0xFF212121).copy(alpha = 0.2f)
                            },
                            topLeft = Offset(x, -h),
                            size = Size(stripeW, h * 3f),
                        )
                    }
                }
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFA000).copy(alpha = 0.2f * pulse),
                            Color.Transparent,
                        ),
                        center = Offset(cx, cy),
                        radius = w * 0.55f,
                    ),
                )
            }
            ConsejoKind.EMERGENCIA -> {
                val alertAlpha = 0.12f + 0.18f * pulse
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFB71C1C).copy(alpha = alertAlpha),
                            Color(0xFF000000).copy(alpha = 0.45f),
                            Color(0xFF4E0000).copy(alpha = 0.35f),
                        ),
                        center = Offset(cx, cy),
                        radius = w * 0.95f,
                    ),
                )
                val scanY = (phase * (h + 80f)) % (h + 80f) - 40f
                drawRect(
                    color = Color(0xFFFF5252).copy(alpha = 0.12f),
                    topLeft = Offset(0f, scanY),
                    size = Size(width = w, height = 28f * density),
                )
                repeat(20) { i ->
                    val ang = (phase * 360.0 + i * 18.0) * PI / 180.0
                    val rr = w * 0.4f
                    drawCircle(
                        color = Color(0xFFFF1744).copy(alpha = 0.08f),
                        radius = 6f + (i % 3) * 4f,
                        center = Offset(
                            cx + cos(ang).toFloat() * rr * 0.6f,
                            cy + sin(ang).toFloat() * rr * 0.5f,
                        ),
                    )
                }
            }
        }
    }
}

private data class Particle(
    val x: Float,
    val y: Float,
    val r: Float,
    val speed: Float,
    val hue: Int,
)
