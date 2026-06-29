package com.antigastos.boludos.ui.achievements

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.antigastos.boludos.AppFeedback
import com.antigastos.boludos.domain.AchievementMeta
import kotlin.math.sin
import kotlin.random.Random

/**
 * Celebración full-bleed con confeti liviano + compartir.
 */
@Composable
fun AchievementUnlockOverlay(
    meta: AchievementMeta,
    pendingExtras: Int = 0,
    soundEnabled: Boolean,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    LaunchedEffect(meta.key) {
        AppFeedback.achievementUnlocked(ctx, soundEnabled)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            ConfettiBackdrop(modifier = Modifier.fillMaxSize())
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "¡Nuevo trofeo!",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${meta.emoji} ${meta.title}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        meta.description,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (pendingExtras > 0) {
                        Text(
                            "+ $pendingExtras trofeo${if (pendingExtras == 1) "" else "s"} más esperándote en la pestaña Trofeos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            AppFeedback.shareAction(ctx)
                            val text =
                                "Desbloqueé «${meta.title}» ${meta.emoji} en Anti-gastos boludos 🇦🇷 #AntiGastosBoludos"
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            ctx.startActivity(Intent.createChooser(send, "Compartir logro"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Text("  Compartir")
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Seguir")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfettiBackdrop(modifier: Modifier = Modifier) {
    val rnd = remember { Random(System.nanoTime()) }
    val specs = remember {
        List(48) {
            Triple(
                rnd.nextFloat(),
                rnd.nextFloat() * 6.2831855f,
                Color(
                    red = 0.4f + rnd.nextFloat() * 0.6f,
                    green = 0.4f + rnd.nextFloat() * 0.6f,
                    blue = 0.5f + rnd.nextFloat() * 0.5f,
                    alpha = 0.85f,
                ),
            )
        }
    }
    val t = rememberInfiniteTransition(label = "confetti")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        for ((baseX, ang, col) in specs) {
            val x = (baseX * w + sin(ang + phase * 12f) * 18f).toFloat()
            val y = ((phase + baseX) % 1f) * (h * 1.2f) - h * 0.1f
            drawCircle(
                color = col,
                radius = 5f + baseX * 6f,
                center = Offset(x, y),
            )
        }
    }
}
