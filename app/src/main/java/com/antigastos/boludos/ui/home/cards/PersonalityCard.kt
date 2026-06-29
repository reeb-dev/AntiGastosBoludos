package com.antigastos.boludos.ui.home.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.domain.CopyMood
import com.antigastos.boludos.domain.PersonaCatalog
import com.antigastos.boludos.domain.model.HomeUiModel
import com.antigastos.boludos.ui.home.PersonalityGif
import com.antigastos.boludos.ui.home.anim.rememberBounceScale
import com.antigastos.boludos.ui.home.anim.rememberShakeOffset
import com.antigastos.boludos.ui.theme.AgColors

/**
 * Card "el personaje habla". Muestra la frase generada por IA o por el
 * motor offline, con micro-animaciones según el `CopyMood` (shake en
 * ALARM/BURN, bounce en CHEER, "respiración" siempre activa).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PersonalityCard(
    state: HomeUiModel,
    aiBusy: Boolean,
    aiUsed: Boolean,
    onShare: () -> Unit,
    onShareDaily: () -> Unit,
    onSpeak: () -> Unit,
    onRegenerate: () -> Unit,
    onShufflePersona: () -> Unit = {},
) {
    val persona = PersonaCatalog.byKey(state.personaKey)
    val mood = state.personalityMood
    val accent = when (mood) {
        CopyMood.CHEER -> AgColors.CheerGreen
        CopyMood.NEUTRAL -> MaterialTheme.colorScheme.primary
        CopyMood.BURN -> AgColors.BurnAmber
        CopyMood.ALARM -> AgColors.AlarmRed
    }

    var pulseTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(state.personalityTemplateId, state.expenseCount, state.lastExpenseSlug) {
        pulseTrigger += 1
    }

    val shakeOffsetDp = rememberShakeOffset(
        triggerKey = pulseTrigger,
        amplitudeDp = when (mood) {
            CopyMood.ALARM -> 10f
            CopyMood.BURN -> 6f
            else -> 0f
        },
    )
    val bounceScale = rememberBounceScale(
        triggerKey = pulseTrigger,
        active = mood == CopyMood.CHEER,
    )

    val infinite = rememberInfiniteTransition(label = "moodBreath")
    val breath by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (mood == CopyMood.ALARM) 1.03f else 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (mood == CopyMood.ALARM) 700 else 1600,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = shakeOffsetDp.dp)
            .scale(bounceScale * breath),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoodDot(color = accent)
                Spacer(Modifier.size(8.dp))
                Text(
                    "${persona.emoji} ${persona.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.size(8.dp))
                AiBadge(aiBusy = aiBusy, aiUsed = aiUsed)
                Spacer(Modifier.weight(1f))
                Text(
                    moodHint(mood),
                    style = MaterialTheme.typography.bodySmall,
                    color = accent,
                )
            }
            AnimatedContent(
                targetState = state.personalityMessage,
                transitionSpec = {
                    (fadeIn(tween(280)) + slideInVertically(
                        animationSpec = tween(280, easing = LinearOutSlowInEasing),
                        initialOffsetY = { it / 3 },
                    ))
                        .togetherWith(fadeOut(tween(180)))
                        .using(SizeTransform(clip = false))
                },
                label = "personalityText",
            ) { msg ->
                Text(msg, style = MaterialTheme.typography.bodyLarge)
            }
            PersonalityGif(
                templateId = state.personalityTemplateId,
                mood = mood,
                slug = state.lastExpenseSlug,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = onSpeak) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Escuchar")
                    Text("  Escuchar")
                }
                TextButton(onClick = onShareDaily) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Text("  Historia 9:16")
                }
                TextButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Compartir")
                    Text("  Frase")
                }
                TextButton(onClick = onShufflePersona) {
                    Text("🎲 Otro personaje")
                }
                if (aiUsed || aiBusy) {
                    TextButton(onClick = onRegenerate, enabled = !aiBusy) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerar")
                        Text("  Otra")
                    }
                }
            }
        }
    }
}

@Composable
private fun AiBadge(aiBusy: Boolean, aiUsed: Boolean) {
    when {
        aiBusy -> {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.small,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 1.5.dp,
                        modifier = Modifier.size(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "pensando…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        aiUsed -> {
            Surface(
                color = Color(0xFF6A1B9A).copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    "IA",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6A1B9A),
                )
            }
        }
    }
}

@Composable
private fun MoodDot(color: Color) {
    val infinite = rememberInfiniteTransition(label = "dotPulse")
    val s by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotS",
    )
    Box(modifier = Modifier.size(12.dp).scale(s)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = color, radius = size.minDimension / 2f)
        }
    }
}

private fun moodHint(mood: CopyMood): String = when (mood) {
    CopyMood.CHEER -> "Bien ahí"
    CopyMood.NEUTRAL -> "Tranqui"
    CopyMood.BURN -> "Cuidadito"
    CopyMood.ALARM -> "Alarma"
}
